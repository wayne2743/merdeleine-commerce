package com.merdeleine.order.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.UUID;

public record CreateOrderRequest(

        @NotNull
        UUID customerId,

        @NotNull
        UUID sellWindowId,

        @NotNull
        UUID productId,

        @NotNull
        @Min(1)
        Integer quantity,

        @NotNull
        @Min(0)
        Integer unitPriceCents,

        @NotBlank
        @Size(max = 10)
        String currency,

        @Valid
        OrderDeliveryRequest delivery,

        @Size(max = 1000)
        String shippingAddress
) {}
