package com.merdeleine.order.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.time.OffsetDateTime;
import java.util.UUID;

public class SellWindowQuotaUpsertDtos {

    public record Request(
            @NotNull UUID sellWindowId,
            @NotNull UUID productId,
            @NotNull @Min(1) Integer minQty,
            @NotNull @Min(1) Integer maxQty
    ) {}

    public record Response(
            UUID id,
            UUID sellWindowId,
            UUID productId,
            Integer minQty,
            Integer maxQty,
            Integer soldQty,
            String status,
            OffsetDateTime updatedAt
    ) {}
}