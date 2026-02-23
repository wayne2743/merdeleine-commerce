package com.merdeleine.catalog.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.time.OffsetDateTime;
import java.util.UUID;

public class AutoGroupOrderDtos {

    public record Request(
            @NotNull UUID productId,
            @NotNull @Min(1) Integer qty,
            String contactName,
            String contactPhone,
            String contactEmail,
            String shippingAddress
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