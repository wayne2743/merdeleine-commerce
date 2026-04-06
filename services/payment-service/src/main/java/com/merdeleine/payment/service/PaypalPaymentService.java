package com.merdeleine.payment.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.merdeleine.enums.PaymentProvider;
import com.merdeleine.enums.PaymentStatus;
import com.merdeleine.payment.config.PaypalProperties;
import com.merdeleine.payment.dto.CapturePaypalPaymentResponse;
import com.merdeleine.payment.dto.CreatePaypalPaymentRequest;
import com.merdeleine.payment.dto.CreatePaypalPaymentResponse;
import com.merdeleine.payment.entity.Payment;
import com.merdeleine.payment.repository.PaymentRepository;
import jakarta.transaction.Transactional;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class PaypalPaymentService {

    private final RestClient restClient;
    private final PaypalProperties paypalProperties;
    private final PaymentRepository paymentRepository;
    private final ObjectMapper objectMapper;
    private final OrderQueryService orderQueryService;

    public PaypalPaymentService(RestClient restClient,
                                PaypalProperties paypalProperties,
                                PaymentRepository paymentRepository,
                                ObjectMapper objectMapper,
                                OrderQueryService orderQueryService) {
        this.restClient = restClient;
        this.paypalProperties = paypalProperties;
        this.paymentRepository = paymentRepository;
        this.objectMapper = objectMapper;
        this.orderQueryService = orderQueryService;
    }

    @Transactional
    public CreatePaypalPaymentResponse createPayment(CreatePaypalPaymentRequest request) {
        OrderSnapshot order = orderQueryService.getPayableOrder(request.getOrderId());

        Payment payment = paymentRepository.findByOrderId(order.orderId()).stream().findFirst()
                .orElseThrow(() -> new IllegalStateException("No payment record found for orderId=" + order.orderId()));

        if (payment.getId() == null) {
            payment.setOrderId(order.orderId());
            payment.setProvider(PaymentProvider.PayPal);
        }

        if (payment.getStatus() == PaymentStatus.PAID) {
            throw new IllegalStateException("Order already paid");
        }

        String accessToken = fetchAccessToken();

        Map<String, Object> payload = buildCreateOrderPayload(order);

        String responseBody = restClient.post()
                .uri(paypalProperties.getBaseUrl() + "/v2/checkout/orders")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .body(payload)
                .retrieve()
                .body(String.class);

        try {
            JsonNode root = objectMapper.readTree(responseBody);
            String paypalOrderId = root.path("id").asText();
            String approveUrl = extractApproveUrl(root);

            payment.setAmountCents(order.amountCents());
            payment.setCurrency(order.currency());
            payment.setProvider(PaymentProvider.PayPal);
            payment.setProviderPaymentId(paypalOrderId);
            payment.setStatus(PaymentStatus.PENDING);
            payment.setExpireAt(OffsetDateTime.now().plusMinutes(30));

            Payment saved = paymentRepository.save(payment);

            return new CreatePaypalPaymentResponse(
                    saved.getId(),
                    paypalOrderId,
                    approveUrl
            );
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse PayPal create order response", e);
        }
    }

    @Transactional
    public CapturePaypalPaymentResponse capturePayment(String paypalOrderId) {
        Payment payment = paymentRepository.findByProviderPaymentId(paypalOrderId)
                .orElseThrow(() -> new IllegalArgumentException("Payment not found by providerPaymentId"));

        if (payment.getStatus() == PaymentStatus.PAID) {
            return new CapturePaypalPaymentResponse(
                    payment.getStatus().name(),
                    payment.getProviderPaymentId(),
                    null
            );
        }

        String accessToken = fetchAccessToken();

        String responseBody = restClient.post()
                .uri(paypalProperties.getBaseUrl() + "/v2/checkout/orders/{orderId}/capture", paypalOrderId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .retrieve()
                .body(String.class);

        try {
            JsonNode root = objectMapper.readTree(responseBody);
            String paypalStatus = root.path("status").asText();
            String captureId = extractCaptureId(root);

            if ("COMPLETED".equalsIgnoreCase(paypalStatus)) {
                payment.setStatus(PaymentStatus.PAID);
            } else {
                payment.setStatus(PaymentStatus.FAILED);
            }

            paymentRepository.save(payment);

            return new CapturePaypalPaymentResponse(
                    payment.getStatus().name(),
                    payment.getProviderPaymentId(),
                    captureId
            );
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse PayPal capture response", e);
        }
    }

    private String fetchAccessToken() {
        String basicToken = Base64.getEncoder()
                .encodeToString((paypalProperties.getClientId() + ":" + paypalProperties.getClientSecret())
                        .getBytes(StandardCharsets.UTF_8));

        String body = restClient.post()
                .uri(paypalProperties.getBaseUrl() + "/v1/oauth2/token")
                .header(HttpHeaders.AUTHORIZATION, "Basic " + basicToken)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body("grant_type=client_credentials")
                .retrieve()
                .body(String.class);

        try {
            JsonNode root = objectMapper.readTree(body);
            return root.path("access_token").asText();
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse PayPal access token", e);
        }
    }

    private Map<String, Object> buildCreateOrderPayload(OrderSnapshot order) {
        String value = String.format("%.2f", order.amountCents() / 100.0);

        Map<String, Object> amount = new HashMap<>();
        amount.put("currency_code", order.currency());
        amount.put("value", value);

        Map<String, Object> purchaseUnit = new HashMap<>();
        purchaseUnit.put("reference_id", order.orderId().toString());
        purchaseUnit.put("custom_id", order.orderId().toString());
        purchaseUnit.put("description", order.description());
        purchaseUnit.put("amount", amount);

        Map<String, Object> applicationContext = new HashMap<>();
        applicationContext.put("brand_name", paypalProperties.getBrandName());
        applicationContext.put("return_url", paypalProperties.getReturnUrl());
        applicationContext.put("cancel_url", paypalProperties.getCancelUrl());
        applicationContext.put("user_action", "PAY_NOW");

        Map<String, Object> payload = new HashMap<>();
        payload.put("intent", "CAPTURE");
        payload.put("purchase_units", java.util.List.of(purchaseUnit));
        payload.put("application_context", applicationContext);

        return payload;
    }

    private String extractApproveUrl(JsonNode root) {
        JsonNode links = root.path("links");
        if (links.isArray()) {
            for (JsonNode link : links) {
                if ("approve".equalsIgnoreCase(link.path("rel").asText())) {
                    return link.path("href").asText();
                }
            }
        }
        return null;
    }

    private String extractCaptureId(JsonNode root) {
        JsonNode purchaseUnits = root.path("purchase_units");
        if (purchaseUnits.isArray() && !purchaseUnits.isEmpty()) {
            JsonNode captures = purchaseUnits.get(0)
                    .path("payments")
                    .path("captures");
            if (captures.isArray() && !captures.isEmpty()) {
                return captures.get(0).path("id").asText(null);
            }
        }
        return null;
    }

    public record OrderSnapshot(UUID orderId, Integer amountCents, String currency, String description) {
    }
}