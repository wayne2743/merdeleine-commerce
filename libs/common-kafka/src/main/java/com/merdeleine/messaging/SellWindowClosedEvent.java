package com.merdeleine.messaging;

import java.time.OffsetDateTime;
import java.util.UUID;

public record SellWindowClosedEvent (
        UUID eventId,
        String eventType,
        UUID sellWindowId,
        UUID productId,
        OffsetDateTime occurredAt
){
}
