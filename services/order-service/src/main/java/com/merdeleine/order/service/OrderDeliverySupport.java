package com.merdeleine.order.service;

import com.merdeleine.order.dto.OrderDeliveryRequest;
import com.merdeleine.order.entity.Order;
import com.merdeleine.order.entity.OrderDelivery;
import com.merdeleine.order.enums.DeliveryMethodType;
import org.springframework.util.StringUtils;

public final class OrderDeliverySupport {

    private OrderDeliverySupport() {
    }

    public static OrderDeliveryRequest resolveForCreate(OrderDeliveryRequest delivery, String shippingAddress) {
        if (delivery != null) {
            return delivery;
        }
        if (StringUtils.hasText(shippingAddress)) {
            return toHomeDelivery(shippingAddress);
        }
        throw new IllegalArgumentException("delivery is required");
    }

    public static OrderDeliveryRequest resolveForUpdate(OrderDeliveryRequest delivery, String shippingAddress) {
        if (delivery != null) {
            return delivery;
        }
        if (StringUtils.hasText(shippingAddress)) {
            return toHomeDelivery(shippingAddress);
        }
        return null;
    }

    public static void applyAndValidate(Order order, OrderDeliveryRequest req) {
        DeliveryMethodType method = req.deliveryMethod();
        if (method == null) {
            throw new IllegalArgumentException("delivery.deliveryMethod is required");
        }

        OrderDelivery delivery = order.getDelivery();
        if (delivery == null) {
            delivery = new OrderDelivery();
            order.setDelivery(delivery);
        }

        delivery.setDeliveryMethod(method);
        delivery.setPickupLocationName(req.pickupLocationName());
        delivery.setPickupLocationAddress(req.pickupLocationAddress());
        delivery.setPickupTime(req.pickupTime());
        delivery.setConvenienceStoreCode(req.convenienceStoreCode());
        delivery.setConvenienceStoreName(req.convenienceStoreName());
        delivery.setConvenienceStoreAddress(req.convenienceStoreAddress());
        delivery.setHomeDeliveryAddress(req.homeDeliveryAddress());

        switch (method) {
            case STORE_PICKUP -> {
                requireText(req.pickupLocationAddress(), "delivery.pickupLocationAddress is required for STORE_PICKUP");
                if (req.pickupTime() == null) {
                    throw new IllegalArgumentException("delivery.pickupTime is required for STORE_PICKUP");
                }
                order.setShippingAddress(req.pickupLocationAddress());
            }
            case CONVENIENCE_STORE_PICKUP -> {
                requireText(req.convenienceStoreCode(), "delivery.convenienceStoreCode is required for CONVENIENCE_STORE_PICKUP");
                requireText(req.convenienceStoreName(), "delivery.convenienceStoreName is required for CONVENIENCE_STORE_PICKUP");
                requireText(req.convenienceStoreAddress(), "delivery.convenienceStoreAddress is required for CONVENIENCE_STORE_PICKUP");
                order.setShippingAddress(req.convenienceStoreAddress());
            }
            case HOME_DELIVERY -> {
                requireText(req.homeDeliveryAddress(), "delivery.homeDeliveryAddress is required for HOME_DELIVERY");
                order.setShippingAddress(req.homeDeliveryAddress());
            }
        }
    }

    private static OrderDeliveryRequest toHomeDelivery(String shippingAddress) {
        return new OrderDeliveryRequest(
                DeliveryMethodType.HOME_DELIVERY,
                null,
                null,
                null,
                null,
                null,
                null,
                shippingAddress
        );
    }

    private static void requireText(String val, String message) {
        if (!StringUtils.hasText(val)) {
            throw new IllegalArgumentException(message);
        }
    }
}

