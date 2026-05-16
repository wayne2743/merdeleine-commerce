package com.merdeleine.order.dto;

import com.merdeleine.order.enums.DeliveryMethodType;

import java.time.OffsetDateTime;
import java.util.UUID;

public record OrderDeliveryResponse(
        DeliveryMethodType deliveryMethod,
        UUID pickupLocationId,
        String pickupLocationName,
        String pickupLocationAddress,
        OffsetDateTime pickupTime,
        String convenienceStoreCode,
        String convenienceStoreName,
        String convenienceStoreAddress,
        String homeDeliveryAddress
) {}

