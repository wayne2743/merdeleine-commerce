package com.merdeleine.messaging;

import java.time.OffsetDateTime;
import java.util.UUID;

public record PaymentExpiredEvent(
        UUID eventId,
        String eventType,
        UUID paymentId,
        UUID orderId,
        OffsetDateTime expiredAt
) {}