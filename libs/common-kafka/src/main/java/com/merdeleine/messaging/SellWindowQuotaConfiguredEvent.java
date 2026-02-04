package com.merdeleine.messaging;

import java.time.OffsetDateTime;
import java.util.UUID;

public record SellWindowQuotaConfiguredEvent (
        UUID eventId,
        String eventType,
        UUID sellWindowId,
        UUID productId,
        int minQty,
        int maxQty,
        OffsetDateTime occurredAt
){}
