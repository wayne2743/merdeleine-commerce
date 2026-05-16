package com.merdeleine.order.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record StorePickupLocationRequest(
        @NotBlank @Size(max = 255) String name,
        @NotBlank @Size(max = 1000) String address,
        @Size(max = 30) String contactPhone,
        Boolean active
) {}

