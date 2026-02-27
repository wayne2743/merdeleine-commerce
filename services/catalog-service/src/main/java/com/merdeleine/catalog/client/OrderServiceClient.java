package com.merdeleine.catalog.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.UUID;

@Component
public class OrderServiceClient {

    private final RestClient restClient;

    public OrderServiceClient(
            RestClient.Builder builder,
            @Value("${app.services.order.base-url}") String baseUrl
    ) {
        this.restClient = builder.baseUrl(baseUrl).build();
    }

    public record AutoReserveRequest(
            UUID sellWindowId,
            UUID productId,
            Integer qty,
            Integer unitPriceCents,
            String currency,
            String contactName,
            String contactPhone,
            String contactEmail,
            String shippingAddress
    ) {}

    public record AutoReserveResponse(
            UUID orderId,
            String status
    ) {}

    public AutoReserveResponse autoReserve(AutoReserveRequest req) {
        return restClient.post()
                .uri("/api/order/orders/auto-reserve")
                .contentType(MediaType.APPLICATION_JSON)
                .body(req)
                .retrieve()
                .body(AutoReserveResponse.class);
    }


    // NEW: upsert quota
    public record UpsertQuotaRequest(
            UUID sellWindowId,
            UUID productId,
            Integer minQty,
            Integer maxQty
    ) {}

    public record UpsertQuotaResponse(
            UUID id,
            UUID sellWindowId,
            UUID productId,
            Integer minQty,
            Integer maxQty,
            Integer soldQty,
            String status
    ) {}

    public UpsertQuotaResponse upsertQuota(UpsertQuotaRequest req) {
        return restClient.put()
                .uri("/order/internal/sell-window-quotas/upsert")
                .contentType(MediaType.APPLICATION_JSON)
                .body(req)
                .retrieve()
                .body(UpsertQuotaResponse.class);
    }
}