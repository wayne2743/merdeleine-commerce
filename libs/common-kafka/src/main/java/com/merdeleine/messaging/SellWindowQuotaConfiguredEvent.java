package com.merdeleine.messaging;

import java.util.UUID;

public record SellWindowQuotaConfiguredEvent (
        UUID eventId,
        String eventType,
        UUID sellWindowId,
        UUID productId,
        UUID variantId,
        int minQty,
        int maxQty
){}
