package com.merdeleine.order.dto;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record OrderResponse(
        UUID id,
        String orderNo,
        UUID customerId,
        UUID sellWindowId,
        String status,
        Integer totalAmountCents,
        String currency,
        String contactName,
        String contactPhone,
        String contactEmail,
        String shippingAddress,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt,
        List<OrderItemResponse> items
) {}
