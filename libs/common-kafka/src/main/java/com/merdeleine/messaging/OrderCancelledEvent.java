package com.merdeleine.messaging;

import java.time.OffsetDateTime;
import java.util.UUID;

public record OrderCancelledEvent(
        UUID eventId,
        String eventType,
        UUID orderId,
        UUID sellWindowId,
        UUID productId,
        int quantity,
        OffsetDateTime occurredAt
) {}

