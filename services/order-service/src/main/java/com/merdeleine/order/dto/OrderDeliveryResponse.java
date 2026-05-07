package com.merdeleine.order.dto;

import com.merdeleine.order.enums.DeliveryMethodType;

import java.time.OffsetDateTime;

public record OrderDeliveryResponse(
        DeliveryMethodType deliveryMethod,
        String pickupLocationName,
        String pickupLocationAddress,
        OffsetDateTime pickupTime,
        String convenienceStoreCode,
        String convenienceStoreName,
        String convenienceStoreAddress,
        String homeDeliveryAddress
) {}

