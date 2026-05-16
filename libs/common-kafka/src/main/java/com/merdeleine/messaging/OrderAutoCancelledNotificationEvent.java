package com.merdeleine.messaging;

import java.time.OffsetDateTime;
import java.util.UUID;

public record OrderAutoCancelledNotificationEvent(
        UUID eventId,
        String eventType,
        UUID orderId,
        String orderNo,
        UUID customerId,
        UUID sellWindowId,
        String cancelReason,
        OffsetDateTime occurredAt
) {
}

