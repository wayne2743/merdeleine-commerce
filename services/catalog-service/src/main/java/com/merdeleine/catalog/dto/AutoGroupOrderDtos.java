package com.merdeleine.catalog.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.OffsetDateTime;
import java.util.UUID;

public class AutoGroupOrderDtos {

    public record Request(
            @NotNull UUID productId,
            @NotNull @Min(1) Integer qty,
            @NotNull UUID customerId,
            @NotBlank @Size(max = 1000) String shippingAddress,
            @NotNull OffsetDateTime predictedGroupOpenAt,
            @NotNull OffsetDateTime predictedGroupEndAt,
            @NotNull @Min(0) Integer leadsDay,
            @NotNull @Min(0) Integer shipDay
    ) {}

    public record Response(
            UUID sellWindowId,
            UUID orderId,
            String status,
            SellWindowInfo sellWindow
    ) {}

    public record SellWindowInfo(
            OffsetDateTime startAt,
            OffsetDateTime endAt,
            String timezone,
            Integer paymentTtlMinutes
    ) {}
}