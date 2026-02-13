package com.merdeleine.messaging;

import java.util.UUID;

public record BatchConfirmEvent (
        UUID eventId,
        String eventType,          // "batch_confirmed.v1"
        UUID batchId,
        UUID productId,
        UUID sellWindowId
){

}
