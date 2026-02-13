package com.merdeleine.messaging;

import java.util.UUID;

public record PaymentRequestedEvent (
        UUID eventId,
        String eventType,
        UUID orderId,
        int totalAmount,
        String currency
){
}
