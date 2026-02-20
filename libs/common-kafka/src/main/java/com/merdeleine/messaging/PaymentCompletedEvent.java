package com.merdeleine.messaging;

import java.time.OffsetDateTime;
import java.util.UUID;

public record PaymentCompletedEvent (
        UUID eventId,
        String eventType,            // "order.quantity_committed.v1"
        UUID orderId,
        String orderNo,
        UUID sellWindowId,
        UUID productId,
        int quantity,
        OffsetDateTime occurredAt

){
}
