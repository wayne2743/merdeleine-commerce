package com.merdeleine.order.dto;

import jakarta.validation.constraints.*;
import java.util.List;
import java.util.UUID;

public record OrderCreateRequest(
        @NotNull UUID customerId,
        UUID sellWindowId,

        @NotBlank @Size(max = 10) String currency,

        @Size(max = 100) String contactName,
        @Size(max = 30) String contactPhone,
        @Email @Size(max = 255) String contactEmail,
        String shippingAddress,

        @NotEmpty List<OrderItemRequest> items
) {}
