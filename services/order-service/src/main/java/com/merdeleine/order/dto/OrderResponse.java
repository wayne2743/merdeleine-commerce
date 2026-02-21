package com.merdeleine.order.dto;

import com.merdeleine.enums.OrderStatus;
import java.time.OffsetDateTime;
import java.util.UUID;

public record OrderResponse(

        UUID orderId,
        String orderNo,
        OrderStatus status,
        UUID sellWindowId,
        UUID productId,
        Integer quantity,
        Integer unitPriceCents,
        Integer subtotalCents,
        Integer totalAmountCents,
        String currency,
        OffsetDateTime createdAt
) {}
