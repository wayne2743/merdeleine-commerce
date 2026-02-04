package com.merdeleine.messaging;

import java.time.OffsetDateTime;
import java.util.UUID;

public record OrderAccumulatedEvent(
        UUID eventId,
        String eventType,            // "order.quantity_committed.v1"
        UUID orderId,
        UUID sellWindowId,
        UUID productId,
        int quantity,
        OffsetDateTime occurredAt
) {}
