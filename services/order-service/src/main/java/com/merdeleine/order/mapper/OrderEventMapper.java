package com.merdeleine.order.mapper;

import com.merdeleine.order.entity.Order;
import com.merdeleine.messaging.OrderReservedEvent;

public class OrderEventMapper {

    public OrderReservedEvent toOrderEvent(Order order) {
        return new OrderReservedEvent(
                java.util.UUID.randomUUID(),
                "order.quantity_committed.v1",
                order.getId(),
                order.getSellWindowId(),
                order.getItem().getProductId(),
                order.getItem().getQuantity(),
                java.time.OffsetDateTime.now()
        );
    }
}
