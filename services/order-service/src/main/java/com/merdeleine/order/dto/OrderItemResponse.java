package com.merdeleine.order.dto;

import java.util.UUID;

public record OrderItemResponse(
        UUID id,
        UUID productId,
        UUID variantId,
        Integer quantity,
        Integer unitPriceCents,
        Integer subtotalCents
) {}
