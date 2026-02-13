package com.merdeleine.messaging;

import java.util.UUID;

public record PaymentCreatedEvent (
        UUID eventId,
        String eventType,
        UUID orderId,
        UUID paymentId
){
}
