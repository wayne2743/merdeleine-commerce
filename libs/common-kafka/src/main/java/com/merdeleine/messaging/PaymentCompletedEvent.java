package com.merdeleine.messaging;

import com.merdeleine.enums.PaymentStatus;

import java.util.UUID;

public record PaymentCompletedEvent(
        UUID eventId,
        String eventType,            // "order.quantity_committed.v1"
        UUID paymentId,
        UUID orderId,
        PaymentStatus paymentStatus
){
}
