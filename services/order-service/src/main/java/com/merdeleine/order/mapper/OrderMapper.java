package com.merdeleine.order.mapper;

import com.merdeleine.order.dto.CreateOrderRequest;
import com.merdeleine.order.dto.OrderResponse;
import com.merdeleine.order.entity.Order;
import com.merdeleine.order.entity.OrderItem;
import com.merdeleine.order.enums.OrderStatus;

import java.util.UUID;

public class OrderMapper {

    public static Order toEntity(CreateOrderRequest req) {

        int subtotal = req.quantity() * req.unitPriceCents();

        Order order = new Order(
                UUID.randomUUID(),
                generateOrderNo(),
                req.customerId(),
                OrderStatus.PENDING_PAYMENT,
                subtotal,
                req.currency()
        );

        order.setSellWindowId(req.sellWindowId());
        order.setContactName(req.contactName());
        order.setContactPhone(req.contactPhone());
        order.setContactEmail(req.contactEmail());
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

        return new OrderResponse(
                order.getId(),
                order.getOrderNo(),
                order.getStatus(),
                order.getSellWindowId(),
                item.getProductId(),
                item.getQuantity(),
                item.getUnitPriceCents(),
                item.getSubtotalCents(),
                order.getTotalAmountCents(),
                order.getCurrency(),
                order.getCreatedAt()
        );
    }

    private static String generateOrderNo() {
        return "ORD-" + System.currentTimeMillis();
    }
}
