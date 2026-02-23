package com.merdeleine.catalog.client;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.web.client.RestClient;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public class OrderQuotaClient {

    private final RestClient restClient;

    public OrderQuotaClient(RestClient restClient) {
        this.restClient = restClient;
    }

    public record Key(UUID sellWindowId, UUID productId) {}
    public record BatchRequest(List<Key> keys) {}
    public record QuotaDto(
            UUID sellWindowId,
            UUID productId,
            Integer minQty,
            Integer maxQty,
            Integer soldQty,
            String status,
            OffsetDateTime updatedAt
    ) {}

    public List<QuotaDto> batchGet(List<Key> keys) {
        var req = new BatchRequest(keys);
        return restClient.post()
                .uri("/order/internal/sell-window-quotas/_batch")
                .body(req)
                .retrieve()
                .body(new ParameterizedTypeReference<List<QuotaDto>>() {});
    }
}