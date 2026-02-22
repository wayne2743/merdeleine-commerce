package com.merdeleine.production.planning.client;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.UUID;

@Component
public class OrderServiceClient {

    private final RestClient restClient;

    public OrderServiceClient(@Qualifier(value = "orderServiceRestClient") RestClient restClient) {
        this.restClient = restClient;
    }


    public CloseQuotaResponse closeQuota(UUID sellWindowId, UUID productId, UUID reasonEventId, String reason) {
        CloseQuotaRequest req = new CloseQuotaRequest(sellWindowId, productId, reasonEventId, reason);

        return restClient.post()
                .uri("/internal/sell-window-quotas/close")
                .contentType(MediaType.APPLICATION_JSON)
                .body(req)
                .retrieve()
                .body(CloseQuotaResponse.class);
    }

    public record CloseQuotaRequest(
            UUID sellWindowId,
            UUID productId,
            UUID reasonEventId,
            String reason
    ) {}

    public record CloseQuotaResponse(
            UUID sellWindowId,
            UUID productId,
            boolean closed,
            String status
    ) {}
}
