package com.merdeleine.catalog.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record ProductSellWindowRow(
        UUID sellWindowId,
        String sellWindowName,
        OffsetDateTime startAt,
        OffsetDateTime endAt,
        String timezone,
        OffsetDateTime paymentCloseAt,

        UUID productId,
        String productName,

        int minQty,
        Integer maxQty
) {}