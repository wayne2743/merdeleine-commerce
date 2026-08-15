package com.merdeleine.messaging;

import com.merdeleine.enums.PaymentStatus;

import java.util.UUID;

public record PaymentFailedEvent(
        UUID eventId,
        String eventType,
        UUID paymentId,
        UUID orderId,
        PaymentStatus paymentStatus
) {
}
