// EcpayController.java
package com.merdeleine.payment.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.merdeleine.payment.ecpay.EcpayCheckMacValue;
import com.merdeleine.payment.ecpay.EcpayProperties;
import com.merdeleine.payment.entity.OutboxEvent;
import com.merdeleine.payment.enums.OutboxEventStatus;
import com.merdeleine.payment.repository.OutboxEventRepository;
import com.merdeleine.payment.service.EcpayCheckoutService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/payments/ecpay")
public class EcpayController {

    private final EcpayCheckoutService checkoutService;
    private final EcpayProperties props;
    private final ObjectMapper objectMapper;
    private final OutboxEventRepository outboxEventRepository;
    private final String paymentCompleteTopic;
    private final String paymentFailedTopic;

    public EcpayController(EcpayCheckoutService checkoutService,
                           EcpayProperties props,
                           ObjectMapper objectMapper,
                           OutboxEventRepository outboxEventRepository,
                           @Value("${app.kafka.topic.payment-completed-events}") String paymentCompleteTopic,
                           @Value("${app.kafka.topic.payment-failed-events}") String paymentFailedTopic) {
        this.checkoutService = checkoutService;
        this.props = props;
        this.objectMapper = objectMapper;
        this.outboxEventRepository = outboxEventRepository;
        this.paymentCompleteTopic = paymentCompleteTopic;
        this.paymentFailedTopic = paymentFailedTopic;
    }

    // Demo：直接用 orderId 產生導轉頁（你實作時應該從 DB 讀金額/品項）
    @GetMapping(value = "/checkout/{orderId}", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> checkout(@PathVariable String orderId) {
        int amount = 1000;
        String itemName = "merdeleine dessert x1";
        String html = checkoutService.buildAutoSubmitHtml(orderId, amount, itemName);
        return ResponseEntity.ok(html);
    }

    // 綠界 ReturnURL：Server POST (x-www-form-urlencoded)
    @PostMapping(value = "/return", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public String ecpayReturn(@RequestBody MultiValueMap<String, String> body) {
        Map<String, String> params = new HashMap<>();
        body.forEach((k, v) -> params.put(k, v == null || v.isEmpty() ? "" : v.get(0)));

        // 1) 驗簽
        String received = params.getOrDefault("CheckMacValue", "");
        String expected = EcpayCheckMacValue.gen(params, props.hashKey(), props.hashIv());
        if (!expected.equalsIgnoreCase(received)) {
            // 驗簽失敗：不要更新訂單
            // 回應不正確，綠界可能會重送；但你也要記 log 方便追
            return "0|FAIL";
        }

        // 2) 根據 RtnCode 更新訂單狀態
        // RtnCode=1 通常表示付款成功（不同付款方式會有不同回傳節點）
        String rtnCode = params.getOrDefault("RtnCode", "");
        String merchantTradeNo = params.getOrDefault("MerchantTradeNo", "");
        // TODO: 查 DB 依 merchantTradeNo 找到你的 payment/order，做 idempotent update

        if ("1".equals(rtnCode)) {
            // TODO: update to PAID, publish payment.completed.v1
            writeOutbox(
                    "PAYMENT",
                    UUID.randomUUID(), // TODO: 改成 orderId 或 paymentId
                    paymentCompleteTopic,
                    Map.of("orderId", merchantTradeNo, "amount", params.getOrDefault("TradeAmt", ""))
            );
        } else {
            // TODO: update to FAILED or PENDING (ATM/超商可能是待繳費資訊)
            writeOutbox(
                    "PAYMENT",
                    UUID.randomUUID(), // TODO: 改成 orderId 或 paymentId
                    paymentFailedTopic,
                    Map.of("orderId", merchantTradeNo, "rtnCode", rtnCode)
            );
        }

        // 3) 務必回這個字串，完全一致：1|OK:contentReference[oaicite:10]{index=10}
        return "1|OK";
    }

    // 給使用者看的結果頁（可先不做）
    @PostMapping(value = "/result", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public String ecpayResult(@RequestBody MultiValueMap<String, String> body) {
        return "付款結果已接收，可回到商店頁面。";
    }

    private void writeOutbox(String aggregateType, UUID aggregateId, String eventType, Object payloadObj) {
        try {
            OutboxEvent evt = new OutboxEvent();
            evt.setId(UUID.randomUUID());
            evt.setAggregateType(aggregateType);
            evt.setAggregateId(aggregateId);
            evt.setEventType(eventType);
            evt.setPayload(objectMapper.valueToTree(payloadObj));
            evt.setStatus(OutboxEventStatus.NEW);
            outboxEventRepository.save(evt);
        } catch (Exception e) {
            // 讓 transaction rollback，確保「業務寫入 + outbox」同生共死
            throw new RuntimeException("Failed to write outbox event", e);
        }
    }

}
