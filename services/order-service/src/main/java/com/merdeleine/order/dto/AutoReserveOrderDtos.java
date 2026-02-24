package com.merdeleine.order.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public class AutoReserveOrderDtos {

    public record Request(
            @NotNull UUID sellWindowId,
            @NotNull UUID productId,
            @NotNull @Min(1) Integer qty,

            String contactName,
            String contactPhone,
            String contactEmail,
            String shippingAddress
    ) {}

    public record Response(
            UUID orderId,
            String status
    ) {}
}