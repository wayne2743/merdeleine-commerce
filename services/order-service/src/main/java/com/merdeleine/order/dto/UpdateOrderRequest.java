package com.merdeleine.order.dto;

import jakarta.validation.constraints.Size;

public record UpdateOrderRequest(

        @Size(max = 100)
        String contactName,

        @Size(max = 30)
        String contactPhone,

        @Size(max = 255)
        String contactEmail,

        @Size(max = 1000)
        String shippingAddress
) {}
