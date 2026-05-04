package com.merdeleine.gatewaybff.internal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;

@Component
@ConditionalOnProperty(prefix = "app.scheduler.sell-window-close-expired", name = "enabled", havingValue = "true", matchIfMissing = true)
public class SellWindowCloseExpiredScheduler {

    private static final Logger log = LoggerFactory.getLogger(SellWindowCloseExpiredScheduler.class);

    private final WebClient webClient;
    private final String catalogBaseUrl;
    private final String internalSecret;
    private final int limit;

    public SellWindowCloseExpiredScheduler(
            WebClient webClient,
            @Value("${app.services.catalog.base-url}") String catalogBaseUrl,
            @Value("${app.internal.secret}") String internalSecret,
            @Value("${app.scheduler.sell-window-close-expired.limit:200}") int limit
    ) {
        this.webClient = webClient;
        this.catalogBaseUrl = catalogBaseUrl;
        this.internalSecret = internalSecret;
        this.limit = limit;
    }

    @Scheduled(
            fixedDelayString = "${app.scheduler.sell-window-close-expired.interval-ms:1800000}",
            initialDelayString = "${app.scheduler.sell-window-close-expired.initial-delay-ms:30000}"
    )
    public void closeExpiredSellWindows() {
        try {
            Map<String, Object> response = webClient.post()
                    .uri(catalogBaseUrl + "/internal/sell-windows/close-expired?limit=" + limit)
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .header("X-Internal-Secret", internalSecret)
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                    .block();

            Object candidateCount = response == null ? null : response.get("candidateCount");
            Object casWonCount = response == null ? null : response.get("casWonCount");
            log.info("[SellWindowCloseExpiredScheduler] Triggered close-expired, candidateCount={}, casWonCount={}",
                    candidateCount, casWonCount);
        } catch (Exception e) {
            // Keep scheduler resilient; next cycle will retry.
            log.error("[SellWindowCloseExpiredScheduler] Failed to call close-expired API", e);
        }
    }
}

