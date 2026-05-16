package com.merdeleine.notification.dto;

import java.util.UUID;

public record OrderLookupResponse(
        UUID orderId,
        String orderNo,
        UUID customerId,
        UUID sellWindowId,
        UUID productId
) {
}

