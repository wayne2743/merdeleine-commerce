package com.merdeleine.order.mapper;

import com.merdeleine.order.entity.Order;
import com.merdeleine.messaging.OrderEvent;

public class OrderEventMapper {

    public OrderEvent toOrderEvent(Order order) {
        return new OrderEvent(
                java.util.UUID.randomUUID(),
                "order.quantity_committed.v1",
                order.getId(),
                order.getSellWindowId(),
                order.getItem().getProductId(),
                order.getItem().getVariantId(),
                order.getItem().getQuantity(),
                java.time.OffsetDateTime.now()
        );
    }
}
