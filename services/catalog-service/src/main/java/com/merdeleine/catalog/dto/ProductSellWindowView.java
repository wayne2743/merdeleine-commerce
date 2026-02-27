package com.merdeleine.catalog.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record ProductSellWindowView(
        UUID productSellWindowId,

        UUID sellWindowId,
        String sellWindowName,
        OffsetDateTime startAt,
        OffsetDateTime endAt,
        String timezone,
        OffsetDateTime paymentCloseAt,

        UUID productId,
        String productName,
        Integer unitPriceCents,
        String currency,

        Integer minQty,
        Integer maxQty,

        Integer soldQty,
        String quotaStatus,
        OffsetDateTime quotaUpdatedAt
) {}