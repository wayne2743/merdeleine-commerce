// EcpayController.java
package com.merdeleine.payment.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.merdeleine.enums.PaymentStatus;
import com.merdeleine.payment.ecpay.EcpayCheckMacValue;
import com.merdeleine.payment.ecpay.EcpayProperties;
import com.merdeleine.payment.entity.OutboxEvent;
import com.merdeleine.payment.entity.Payment;
import com.merdeleine.payment.enums.OutboxEventStatus;
import com.merdeleine.payment.mapper.PaymentMapper;
import com.merdeleine.payment.repository.OutboxEventRepository;
import com.merdeleine.payment.repository.PaymentRepository;
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


    public EcpayController(EcpayCheckoutService checkoutService,
                           EcpayProperties props
                           ) {
        this.checkoutService = checkoutService;
        this.props = props;

    }

    // Demo：直接用 providerPaymentId 產生導轉頁（你實作時應該從 DB 讀金額/品項）
    @GetMapping(value = "/checkout/{providerPaymentId}", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> checkout(@PathVariable String providerPaymentId) {
        int amount = 1000;
        String itemName = "merdeleine dessert x1";
        String html = checkoutService.buildAutoSubmitHtml(providerPaymentId, amount, itemName);
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


        checkoutService.preparePaymentCompletedEvent(merchantTradeNo, rtnCode);

        // 3) 務必回這個字串，完全一致：1|OK:contentReference[oaicite:10]{index=10}
        return "1|OK";
    }



    // 給使用者看的結果頁（可先不做）
    @PostMapping(value = "/result", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public String ecpayResult(@RequestBody MultiValueMap<String, String> body) {
        return "付款結果已接收，可回到商店頁面。";
    }


}
