package com.merdeleine.order.mapper;

import com.merdeleine.order.dto.CreateOrderRequest;
import com.merdeleine.order.dto.OrderDeliveryResponse;
import com.merdeleine.order.dto.OrderResponse;
import com.merdeleine.order.entity.OrderDelivery;
import com.merdeleine.order.entity.Order;
import com.merdeleine.order.entity.OrderItem;
import com.merdeleine.enums.OrderStatus;

import java.util.UUID;

public class OrderMapper {

    public static Order toEntity(CreateOrderRequest req, OrderStatus status) {

        int subtotal = req.quantity() * req.unitPriceCents();

        Order order = new Order(
                UUID.randomUUID(),
                generateOrderNo(),
                req.customerId(),
                status,
                subtotal,
                req.currency()
        );

        order.setSellWindowId(req.sellWindowId());
        order.setShippingAddress(req.shippingAddress());

        OrderItem item = new OrderItem(
                UUID.randomUUID(),
                req.productId(),
                req.quantity(),
                req.unitPriceCents(),
                subtotal
        );

        order.setItem(item); // 🔴 關鍵：雙向關聯

        return order;
    }

    public static OrderResponse toResponse(Order order) {

        OrderItem item = order.getItem();
        OrderDelivery delivery = order.getDelivery();

        return new OrderResponse(
                order.getId(),
                order.getOrderNo(),
                order.getCustomerId(),
                order.getStatus(),
                order.getSellWindowId(),
                item != null ? item.getProductId() : null,
                item != null ? item.getQuantity() : null,
                item != null ? item.getUnitPriceCents() : null,
                item != null ? item.getSubtotalCents() : null,
                order.getTotalAmountCents(),
                order.getCurrency(),
                delivery != null
                        ? new OrderDeliveryResponse(
                        delivery.getDeliveryMethod(),
                        delivery.getPickupLocationName(),
                        delivery.getPickupLocationAddress(),
                        delivery.getPickupTime(),
                        delivery.getConvenienceStoreCode(),
                        delivery.getConvenienceStoreName(),
                        delivery.getConvenienceStoreAddress(),
                        delivery.getHomeDeliveryAddress()
                )
                        : null,
                order.getShippingAddress(),
                order.getPaymentDueAt(),
                order.getPaymentFailedCount(),
                order.getLastPaymentError(),
                order.getCreatedAt(),
                order.getUpdatedAt()
        );
    }

    private static String generateOrderNo() {
        return "ORD-" + System.currentTimeMillis();
    }
}
