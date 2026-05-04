package com.merdeleine.gatewaybff.internal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;

@Component
@ConditionalOnProperty(prefix = "app.scheduler.payment-expiry", name = "enabled", havingValue = "true", matchIfMissing = true)
public class PaymentExpiryScheduler {

    private static final Logger log = LoggerFactory.getLogger(PaymentExpiryScheduler.class);

    private final WebClient webClient;
    private final String paymentBaseUrl;
    private final String internalSecret;
    private final int batchSize;

    public PaymentExpiryScheduler(
            WebClient webClient,
            @Value("${app.services.payment.base-url}") String paymentBaseUrl,
            @Value("${app.internal.secret}") String internalSecret,
            @Value("${app.scheduler.payment-expiry.batch-size:200}") int batchSize
    ) {
        this.webClient = webClient;
        this.paymentBaseUrl = paymentBaseUrl;
        this.internalSecret = internalSecret;
        this.batchSize = batchSize;
    }

    @Scheduled(
            fixedDelayString = "${app.scheduler.payment-expiry.interval-ms:1800000}",
            initialDelayString = "${app.scheduler.payment-expiry.initial-delay-ms:30000}"
    )
    public void expireDuePayments() {
        try {
            Map<String, Object> response = webClient.post()
                    .uri(paymentBaseUrl + "/internal/payments/expire-due?batchSize=" + batchSize)
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .header("X-Internal-Secret", internalSecret)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .cast((Class<Map<String, Object>>) (Class<?>) Map.class)
                    .block();

            Object expiredCount = response == null ? null : response.get("expiredCount");
            log.info("[PaymentExpiryScheduler] Triggered payment expiry scan, expiredCount={}", expiredCount);
        } catch (Exception e) {
            // Keep scheduler resilient; next cycle will retry.
            log.error("[PaymentExpiryScheduler] Failed to call payment expiry API", e);
        }
    }
}


