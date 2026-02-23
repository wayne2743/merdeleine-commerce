package com.merdeleine.order.dto;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public class SellWindowQuotaBatchDto {

    public record Key(UUID sellWindowId, UUID productId) {}

    public record BatchRequest(List<Key> keys) {}

    public record QuotaResponse(
            UUID sellWindowId,
            UUID productId,
            Integer minQty,
            Integer maxQty,
            Integer soldQty,
            String status,
            OffsetDateTime updatedAt
    ) {}
}