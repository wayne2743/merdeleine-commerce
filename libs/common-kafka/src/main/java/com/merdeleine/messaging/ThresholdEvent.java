package com.merdeleine.messaging;

import java.util.UUID;

public record ThresholdEvent (
        UUID eventId,
        String eventType,            // "threshold.reached.v1"
        UUID productId,
        UUID sellWindowId,
        int totalQuantity,
        OrderDetails orderDetails
){
    public record OrderDetails (
            UUID orderId,
            UUID variantId,
            int quantity
    ){}
}
