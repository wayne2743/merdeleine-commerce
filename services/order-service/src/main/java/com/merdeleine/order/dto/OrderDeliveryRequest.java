package com.merdeleine.order.dto;

import com.merdeleine.order.enums.DeliveryMethodType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.OffsetDateTime;
import java.util.UUID;

public record OrderDeliveryRequest(
        @NotNull DeliveryMethodType deliveryMethod,
        UUID pickupLocationId,
        @Size(max = 255) String pickupLocationName,
        @Size(max = 1000) String pickupLocationAddress,
        OffsetDateTime pickupTime,
        @Size(max = 50) String convenienceStoreCode,
        @Size(max = 255) String convenienceStoreName,
        @Size(max = 1000) String convenienceStoreAddress,
        @Size(max = 1000) String homeDeliveryAddress
) {}

