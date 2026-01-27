package com.merdeleine.order.dto;

import jakarta.validation.constraints.*;
import java.util.UUID;

public record OrderItemRequest(
        @NotNull UUID productId,
        @NotNull UUID variantId,
        @NotNull @Min(1) Integer quantity,
        @NotNull @Min(0) Integer unitPriceCents
) {}
