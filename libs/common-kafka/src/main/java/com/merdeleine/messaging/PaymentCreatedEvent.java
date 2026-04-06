package com.merdeleine.messaging;

import com.merdeleine.enums.PaymentProvider;

import java.time.OffsetDateTime;
import java.util.UUID;

public record PaymentCreatedEvent (
        UUID eventId,
        String eventType,
        UUID orderId,
        UUID paymentId,
        UUID customerId,
        String providerPaymentId,
        int totalAmount,
        PaymentProvider paymentProvider,
        OffsetDateTime expireAt
){
}
