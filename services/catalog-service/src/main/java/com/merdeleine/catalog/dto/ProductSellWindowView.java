package com.merdeleine.catalog.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record ProductSellWindowView(
        UUID sellWindowId,
        String sellWindowName,
        OffsetDateTime startAt,
        OffsetDateTime endAt,
        String timezone,
        OffsetDateTime paymentCloseAt,

        UUID productId,
        String productName,

        int minQty,
        Integer maxQty,

        int soldQty,
        String quotaStatus,
        OffsetDateTime quotaUpdatedAt
) {}