package com.merdeleine.messaging;

import java.util.UUID;

public record ThresholdReachedEvent(
        UUID eventId,
        String eventType,            // "threshold.reached.v1"
        UUID productId,
        UUID sellWindowId,
        int totalQuantity
){}
