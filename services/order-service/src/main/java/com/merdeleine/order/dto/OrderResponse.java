package com.merdeleine.order.dto;

import com.merdeleine.order.enums.OrderStatus;
import java.time.OffsetDateTime;
import java.util.UUID;

public record OrderResponse(

        UUID orderId,
        String orderNo,
        OrderStatus status,

        UUID sellWindowId,

        UUID productId,
        UUID variantId,
        Integer quantity,
        Integer unitPriceCents,
        Integer subtotalCents,

        Integer totalAmountCents,
        String currency,

        OffsetDateTime createdAt
) {}
