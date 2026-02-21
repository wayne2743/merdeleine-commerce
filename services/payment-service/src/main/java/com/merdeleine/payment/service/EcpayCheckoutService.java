// EcpayCheckoutService.java
package com.merdeleine.payment.service;

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
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class EcpayCheckoutService {

    private final Logger logger = org.slf4j.LoggerFactory.getLogger(EcpayCheckoutService.class);

    private static final DateTimeFormatter ECPAY_DT = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");

    private final EcpayProperties props;
    private final ObjectMapper objectMapper;
    private final OutboxEventRepository outboxEventRepository;
    private final PaymentRepository paymentRepository;
    private final String paymentCompleteTopic;
    private final String paymentFailedTopic;


    public EcpayCheckoutService(EcpayProperties props,
                                ObjectMapper objectMapper,
                                OutboxEventRepository outboxEventRepository,
                                PaymentRepository paymentRepository,
                                @Value("${app.kafka.topic.payment-completed-events}") String paymentCompleteTopic,
                                @Value("${app.kafka.topic.payment-failed-events}") String paymentFailedTopic) {
        this.props = props;
        this.objectMapper = objectMapper;
        this.outboxEventRepository = outboxEventRepository;
        this.paymentRepository = paymentRepository;
        this.paymentCompleteTopic = paymentCompleteTopic;
        this.paymentFailedTopic = paymentFailedTopic;
    }

    public String buildAutoSubmitHtml(String merchantTradeNo, int totalAmount, String itemName) {
        // 你自己的交易號（務必唯一；且符合綠界格式限制）
//        String merchantTradeNo = "M" + orderId.replace("-", "").substring(0, 20);

        String returnUrl = props.publicBaseUrl() + "/payments/ecpay/return";
        String orderResultUrl = props.publicBaseUrl() + "/payments/ecpay/result";
        String clientBackUrl = props.publicBaseUrl() + "/"; // 回首頁

        Map<String, String> form = new LinkedHashMap<>();
        form.put("MerchantID", props.merchantId());
        form.put("MerchantTradeNo", merchantTradeNo);
        form.put("MerchantTradeDate", OffsetDateTime.now().format(ECPAY_DT));
        form.put("PaymentType", "aio");
        form.put("TotalAmount", String.valueOf(totalAmount));
        form.put("TradeDesc", "merdeleine order " + merchantTradeNo);
        form.put("ItemName", itemName);
        form.put("ChoosePayment", "ALL"); // 讓消費者選付款方式:contentReference[oaicite:8]{index=8}
        form.put("ReturnURL", returnUrl);
        form.put("OrderResultURL", orderResultUrl);
        form.put("ClientBackURL", clientBackUrl);
        form.put("EncryptType", "1"); // SHA256:contentReference[oaicite:9]{index=9}

        String checkMac = EcpayCheckMacValue.gen(form, props.hashKey(), props.hashIv());
        form.put("CheckMacValue", checkMac);

        return buildHtml(props.cashierUrl(), form);
    }

    private static String buildHtml(String actionUrl, Map<String, String> form) {
        StringBuilder sb = new StringBuilder();
        sb.append("<html><body onload='document.forms[0].submit()'>");
        sb.append("<form method='post' action='").append(actionUrl).append("'>");
        for (var e : form.entrySet()) {
            sb.append("<input type='hidden' name='")
                    .append(escape(e.getKey()))
                    .append("' value='")
                    .append(escape(e.getValue()))
                    .append("'/>");
        }
        sb.append("</form></body></html>");
        return sb.toString();
    }

    @Transactional
    public void preparePaymentCompletedEvent(String merchantTradeNo, String rtnCode) {
        Payment payment = paymentRepository.findByProviderPaymentId(merchantTradeNo)
                .orElseThrow(()->new RuntimeException("Payment not found for providerPaymentId: " + merchantTradeNo));

        if(payment.getStatus() != PaymentStatus.INIT) {
            // 已經處理過的 payment 就不要重複發 event 了
            logger.info("Payment with providerPaymentId {} already in status {}, skip processing", merchantTradeNo, payment.getStatus());
            return;
        }

        if ("1".equals(rtnCode)) {
            // TODO: update to PAID, publish payment.completed.v1

            payment.setStatus(PaymentStatus.SUCCEEDED);
            writeOutbox(
                    "PAYMENT",
                    payment.getId(),
                    paymentCompleteTopic,
                    PaymentMapper.toPaymentEvent(payment, paymentCompleteTopic)
            );
        } else {
            // TODO: update to FAILED or PENDING (ATM/超商可能是待繳費資訊)
            payment.setStatus(PaymentStatus.FAILED);
            writeOutbox(
                    "PAYMENT",
                    payment.getId(),
                    paymentFailedTopic,
                    PaymentMapper.toPaymentEvent(payment, paymentFailedTopic)
            );
        }
    }

    private static String escape(String s) {
        return s == null ? "" : s.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;")
                .replace("\"","&quot;").replace("'","&#39;");
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
