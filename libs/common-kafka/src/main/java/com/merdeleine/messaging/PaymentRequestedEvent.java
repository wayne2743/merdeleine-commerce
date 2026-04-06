package com.merdeleine.messaging;

import com.merdeleine.enums.PaymentProvider;

import java.time.OffsetDateTime;
import java.util.UUID;

public record PaymentRequestedEvent (
        UUID eventId,
        String eventType,
        UUID orderId,
        UUID customerId,
        Integer totalAmount,
        String currency,
        OffsetDateTime expiresAt,
        PaymentProvider provider
){
}
