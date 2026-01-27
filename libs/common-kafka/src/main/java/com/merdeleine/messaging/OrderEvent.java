package com.merdeleine.messaging;

import java.time.OffsetDateTime;
import java.util.UUID;

public record OrderEvent(
        UUID eventId,
        String eventType,            // "order.quantity_committed.v1"
        UUID orderId,
        UUID sellWindowId,
        UUID productId,
        UUID variantId,
        int quantity,
        OffsetDateTime occurredAt
) {}
