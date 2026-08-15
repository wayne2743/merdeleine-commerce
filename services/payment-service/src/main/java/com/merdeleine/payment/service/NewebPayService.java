package com.merdeleine.payment.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.merdeleine.enums.PaymentProvider;
import com.merdeleine.enums.PaymentStatus;
import com.merdeleine.payment.entity.OutboxEvent;
import com.merdeleine.payment.entity.Payment;
import com.merdeleine.payment.entity.PaymentTxn;
import com.merdeleine.payment.enums.OutboxEventStatus;
import com.merdeleine.payment.enums.PaymentTxnAction;
import com.merdeleine.payment.enums.PaymentTxnResult;
import com.merdeleine.payment.mapper.PaymentMapper;
import com.merdeleine.payment.newebpay.NewebPayCrypto;
import com.merdeleine.payment.newebpay.NewebPayProperties;
import com.merdeleine.payment.repository.OutboxEventRepository;
import com.merdeleine.payment.repository.PaymentRepository;
import com.merdeleine.payment.repository.PaymentTxnRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Locale;
import java.util.UUID;

@Service
public class NewebPayService {


    private final NewebPayProperties properties;
    private final PaymentRepository paymentRepository;
    private final PaymentTxnRepository paymentTxnRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;
    private final String paymentCompleteTopic;
    private final String paymentFailedTopic;

    public NewebPayService(NewebPayProperties properties,
                           PaymentRepository paymentRepository,
                           PaymentTxnRepository paymentTxnRepository,
                           OutboxEventRepository outboxEventRepository,
                           ObjectMapper objectMapper,
                           @Value("${app.kafka.topic.payment-completed-events}") String paymentCompleteTopic,
                           @Value("${app.kafka.topic.payment-failed-events}") String paymentFailedTopic) {
        this.properties = properties;
        this.paymentRepository = paymentRepository;
        this.paymentTxnRepository = paymentTxnRepository;
        this.outboxEventRepository = outboxEventRepository;
        this.objectMapper = objectMapper;
        this.paymentCompleteTopic = paymentCompleteTopic;
        this.paymentFailedTopic = paymentFailedTopic;
    }

    @Transactional
    public String buildCheckoutHtml(UUID paymentId, String email, String itemDescription) {
        properties.requireConfigured();
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Payment not found: " + paymentId));

        requireNewebPay(payment);
        if (payment.getStatus() != PaymentStatus.INIT && payment.getStatus() != PaymentStatus.PENDING) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Payment cannot be checked out in status " + payment.getStatus()
            );
        }

        int amountTwd = toTwdAmount(payment.getAmountCents(), payment.getCurrency());
        String description = normalizeDescription(itemDescription, payment.getOrderId());

        Map<String, String> trade = new LinkedHashMap<>();
        trade.put("MerchantID", properties.merchantId());
        trade.put("RespondType", "JSON");
        trade.put("TimeStamp", String.valueOf(Instant.now().getEpochSecond()));
        trade.put("Version", properties.mpgVersion());
        trade.put("MerchantOrderNo", payment.getProviderPaymentId());
        trade.put("Amt", String.valueOf(amountTwd));
        trade.put("ItemDesc", description);
        trade.put("Email", email);
        trade.put("ReturnURL", properties.callbackBaseUrl() + "/payments/newebpay/return");
        trade.put("NotifyURL", properties.callbackBaseUrl() + "/payments/newebpay/notify");
        trade.put("ClientBackURL", properties.frontendReturnUrl());
        trade.put("CREDIT", "1");

        String tradeInfo = NewebPayCrypto.encrypt(
                NewebPayCrypto.formEncode(trade), properties.hashKey(), properties.hashIv()
        );
        String tradeSha = NewebPayCrypto.tradeSha(tradeInfo, properties.hashKey(), properties.hashIv());

        Map<String, String> form = new LinkedHashMap<>();
        form.put("MerchantID", properties.merchantId());
        form.put("TradeInfo", tradeInfo);
        form.put("TradeSha", tradeSha);
        form.put("Version", properties.mpgVersion());
        form.put("EncryptType", "0");

        payment.setStatus(PaymentStatus.PENDING);
        payment.setApproveUrl(properties.callbackBaseUrl() + "/payments/newebpay/checkout/" + payment.getId());
        paymentRepository.save(payment);

        return autoSubmitHtml(properties.mpgUrl(), form);
    }

    @Transactional
    public CallbackResult handleCallback(Map<String, String> form) {
        properties.requireConfigured();
        String merchantId = required(form, "MerchantID");
        String tradeInfo = required(form, "TradeInfo");
        String tradeSha = required(form, "TradeSha");

        if (!properties.merchantId().equals(merchantId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unexpected NewebPay MerchantID");
        }

        String expectedTradeSha = NewebPayCrypto.tradeSha(
                tradeInfo, properties.hashKey(), properties.hashIv()
        );
        if (!NewebPayCrypto.secureEquals(expectedTradeSha, tradeSha.toUpperCase(Locale.ROOT))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid NewebPay TradeSha");
        }

        JsonNode payload = decryptPayload(tradeInfo);
        JsonNode result = normalizeResult(payload.path("Result"));
        String merchantOrderNo = result.path("MerchantOrderNo").asText();
        if (merchantOrderNo.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Missing MerchantOrderNo in NewebPay callback");
        }

        Payment payment = paymentRepository.findByProviderPaymentId(merchantOrderNo)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Payment not found for MerchantOrderNo " + merchantOrderNo
                ));
        requireNewebPay(payment);

        String resultMerchantId = result.path("MerchantID").asText();
        if (!resultMerchantId.isBlank() && !properties.merchantId().equals(resultMerchantId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "NewebPay callback MerchantID mismatch");
        }

        int callbackAmountTwd = result.path("Amt").asInt(-1);
        if (callbackAmountTwd != toTwdAmount(payment.getAmountCents(), payment.getCurrency())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "NewebPay callback amount mismatch");
        }

        String status = payload.path("Status").asText();
        boolean success = "SUCCESS".equals(status);
        String tradeNo = result.path("TradeNo").asText();

        if (success && isSuccessfulOrRefunded(payment.getStatus())) {
            if (payment.getProviderCaptureId() != null
                    && !payment.getProviderCaptureId().isBlank()
                    && !payment.getProviderCaptureId().equals(tradeNo)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "NewebPay callback TradeNo mismatch");
            }
            return new CallbackResult(payment.getId(), merchantOrderNo, status, true);
        }

        if (success) {
            if (tradeNo.isBlank()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Missing TradeNo in successful callback");
            }
            payment.setProviderCaptureId(tradeNo);
            payment.setStatus(PaymentStatus.SUCCEEDED);
            writeOutbox(paymentCompleteTopic, payment);
        } else if (payment.getStatus() == PaymentStatus.INIT || payment.getStatus() == PaymentStatus.PENDING) {
            payment.setStatus(PaymentStatus.FAILED);
            writeOutbox(paymentFailedTopic, payment);
        } else {
            return new CallbackResult(payment.getId(), merchantOrderNo, status, false);
        }

        paymentRepository.save(payment);
        saveTransaction(payment, success, payload);
        return new CallbackResult(payment.getId(), merchantOrderNo, status, success);
    }

    public static int toTwdAmount(int amountTwd, String currency) {
        if (!"TWD".equalsIgnoreCase(currency)) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "NewebPay checkout only supports TWD");
        }
        if (amountTwd <= 0) {
            throw new ResponseStatusException(
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    "TWD amount must be positive"
            );
        }
        return amountTwd;
    }

    private void requireNewebPay(Payment payment) {
        if (payment.getProvider() != PaymentProvider.NewebPay) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Payment provider is not NewebPay");
        }
        if (payment.getProviderPaymentId() == null || payment.getProviderPaymentId().isBlank()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Payment has no MerchantOrderNo");
        }
    }

    private JsonNode decryptPayload(String tradeInfo) {
        try {
            String decrypted = NewebPayCrypto.decrypt(tradeInfo, properties.hashKey(), properties.hashIv());
            return objectMapper.readTree(decrypted);
        } catch (JsonProcessingException | IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid NewebPay TradeInfo", ex);
        }
    }

    private JsonNode normalizeResult(JsonNode result) {
        if (!result.isTextual()) {
            return result;
        }
        try {
            return objectMapper.readTree(result.asText());
        } catch (JsonProcessingException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid NewebPay Result", ex);
        }
    }

    private void saveTransaction(Payment payment, boolean success, JsonNode payload) {
        PaymentTxn transaction = new PaymentTxn();
        transaction.setId(UUID.randomUUID());
        transaction.setPayment(payment);
        transaction.setAction(PaymentTxnAction.CAPTURE);
        transaction.setResult(success ? PaymentTxnResult.OK : PaymentTxnResult.NG);
        transaction.setRawResponse(payload.toString());
        paymentTxnRepository.save(transaction);
    }

    private void writeOutbox(String eventType, Payment payment) {
        OutboxEvent event = new OutboxEvent();
        event.setId(UUID.randomUUID());
        event.setAggregateType("PAYMENT");
        event.setAggregateId(payment.getId());
        event.setEventType(eventType);
        Object payload = eventType.equals(paymentFailedTopic)
                ? PaymentMapper.toPaymentFailedEvent(payment, eventType)
                : PaymentMapper.toPaymentEvent(payment, eventType);
        event.setPayload(objectMapper.valueToTree(payload));
        event.setStatus(OutboxEventStatus.NEW);
        outboxEventRepository.save(event);
    }

    private static boolean isSuccessfulOrRefunded(PaymentStatus status) {
        return status == PaymentStatus.SUCCEEDED
                || status == PaymentStatus.PARTIALLY_REFUNDED
                || status == PaymentStatus.REFUNDED;
    }

    private static String required(Map<String, String> form, String name) {
        String value = form.get(name);
        if (value == null || value.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Missing NewebPay field: " + name);
        }
        return value;
    }

    private static String normalizeDescription(String itemDescription, UUID orderId) {
        String value = itemDescription == null || itemDescription.isBlank()
                ? "merdeleine order " + orderId
                : itemDescription.trim();
        return value.length() <= 50 ? value : value.substring(0, 50);
    }

    private static String autoSubmitHtml(String actionUrl, Map<String, String> fields) {
        StringBuilder html = new StringBuilder("<!doctype html><html><head><meta charset=\"UTF-8\"></head>")
                .append("<body onload=\"document.forms[0].submit()\">")
                .append("<form method=\"post\" action=\"").append(escapeHtml(actionUrl)).append("\">");
        fields.forEach((name, value) -> html.append("<input type=\"hidden\" name=\"")
                .append(escapeHtml(name)).append("\" value=\"")
                .append(escapeHtml(value)).append("\">")
        );
        return html.append("<noscript><button type=\"submit\">前往付款</button></noscript></form></body></html>")
                .toString();
    }

    private static String escapeHtml(String value) {
        return value.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    public record CallbackResult(UUID paymentId, String merchantOrderNo, String status, boolean success) {
    }
}
