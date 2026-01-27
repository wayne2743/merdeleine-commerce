package com.merdeleine.order.dto;

import jakarta.validation.constraints.*;
import java.util.List;
import java.util.UUID;

public record OrderUpdateRequest(
        UUID sellWindowId,

        @Size(max = 100) String contactName,
        @Size(max = 30) String contactPhone,
        @Email @Size(max = 255) String contactEmail,
        String shippingAddress,

        // 簡化：更新時整包替換 items（實務可做 patch）
        @NotEmpty List<OrderItemRequest> items
) {}
