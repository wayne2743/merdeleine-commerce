package com.merdeleine.messaging;

import com.merdeleine.enums.OrderStatus;

import java.time.OffsetDateTime;
import java.util.UUID;

public record PaymentPaidEvent (
        UUID eventId,
        String eventType,            // "order.quantity_committed.v1"
        UUID orderId,
        UUID sellWindowId,
        UUID productId,
        int quantity,
        OrderStatus orderStatus,
        OffsetDateTime occurredAt
){
}
