package com.merdeleine.messaging;

import java.util.UUID;

public record BatchCreatedNotificationEvent(
        UUID eventId,
        String eventType,            // "batch.created.notification.v1"
        UUID batchId,
        UUID productId,
        UUID sellWindowId,
        int quantity
){}
