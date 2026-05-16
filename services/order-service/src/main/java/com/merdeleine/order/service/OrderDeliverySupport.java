package com.merdeleine.order.service;

import com.merdeleine.order.dto.OrderDeliveryRequest;
import com.merdeleine.order.entity.Order;
import com.merdeleine.order.entity.OrderDelivery;
import com.merdeleine.order.entity.StorePickupLocation;
import com.merdeleine.order.enums.DeliveryMethodType;
import com.merdeleine.order.exception.BadRequestException;
import com.merdeleine.order.exception.ResourceNotFoundException;
import com.merdeleine.order.repository.StorePickupLocationRepository;
import org.springframework.util.StringUtils;

import java.util.UUID;

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

    public static void applyAndValidate(Order order,
                                        OrderDeliveryRequest req,
                                        StorePickupLocationRepository storePickupLocationRepository) {
        DeliveryMethodType method = req.deliveryMethod();
        if (method == null) {
            throw new BadRequestException("delivery.deliveryMethod is required");
        }

        OrderDelivery delivery = order.getDelivery();
        if (delivery == null) {
            delivery = new OrderDelivery();
            order.setDelivery(delivery);
        }

        delivery.setDeliveryMethod(method);
        delivery.setPickupLocationId(req.pickupLocationId());
        delivery.setPickupLocationName(req.pickupLocationName());
        delivery.setPickupLocationAddress(req.pickupLocationAddress());
        delivery.setPickupTime(req.pickupTime());
        delivery.setConvenienceStoreCode(req.convenienceStoreCode());
        delivery.setConvenienceStoreName(req.convenienceStoreName());
        delivery.setConvenienceStoreAddress(req.convenienceStoreAddress());
        delivery.setHomeDeliveryAddress(req.homeDeliveryAddress());

        switch (method) {
            case STORE_PICKUP -> {
                UUID pickupLocationId = req.pickupLocationId();
                if (pickupLocationId == null) {
                    throw new BadRequestException("delivery.pickupLocationId is required for STORE_PICKUP");
                }

                StorePickupLocation location = storePickupLocationRepository.findById(pickupLocationId)
                        .orElseThrow(() -> new ResourceNotFoundException("Store pickup location not found: " + pickupLocationId));
                if (!location.isActive()) {
                    throw new BadRequestException("Selected store pickup location is inactive: " + pickupLocationId);
                }

                delivery.setPickupLocationId(location.getId());
                delivery.setPickupLocationName(location.getName());
                delivery.setPickupLocationAddress(location.getAddress());
                delivery.setConvenienceStoreCode(null);
                delivery.setConvenienceStoreName(null);
                delivery.setConvenienceStoreAddress(null);
                delivery.setHomeDeliveryAddress(null);

                if (req.pickupTime() == null) {
                    throw new BadRequestException("delivery.pickupTime is required for STORE_PICKUP");
                }
                order.setShippingAddress(location.getAddress());
            }
            case CONVENIENCE_STORE_PICKUP -> {
                requireText(req.convenienceStoreCode(), "delivery.convenienceStoreCode is required for CONVENIENCE_STORE_PICKUP");
                requireText(req.convenienceStoreName(), "delivery.convenienceStoreName is required for CONVENIENCE_STORE_PICKUP");
                requireText(req.convenienceStoreAddress(), "delivery.convenienceStoreAddress is required for CONVENIENCE_STORE_PICKUP");
                delivery.setPickupLocationId(null);
                delivery.setPickupLocationName(null);
                delivery.setPickupLocationAddress(null);
                delivery.setPickupTime(null);
                delivery.setHomeDeliveryAddress(null);
                order.setShippingAddress(req.convenienceStoreAddress());
            }
            case HOME_DELIVERY -> {
                requireText(req.homeDeliveryAddress(), "delivery.homeDeliveryAddress is required for HOME_DELIVERY");
                delivery.setPickupLocationId(null);
                delivery.setPickupLocationName(null);
                delivery.setPickupLocationAddress(null);
                delivery.setPickupTime(null);
                delivery.setConvenienceStoreCode(null);
                delivery.setConvenienceStoreName(null);
                delivery.setConvenienceStoreAddress(null);
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
                null,
                shippingAddress
        );
    }

    private static void requireText(String val, String message) {
        if (!StringUtils.hasText(val)) {
            throw new BadRequestException(message);
        }
    }
}

