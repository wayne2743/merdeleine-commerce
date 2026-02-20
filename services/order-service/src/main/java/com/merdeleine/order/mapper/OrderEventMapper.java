package com.merdeleine.order.mapper;

import com.merdeleine.order.entity.Order;
import com.merdeleine.messaging.OrderReservedEvent;

public class OrderEventMapper {

    public OrderReservedEvent toOrderEvent(Order order, String eventType) {
        return new OrderReservedEvent(
                java.util.UUID.randomUUID(),
                eventType,
                order.getId(),
                order.getSellWindowId(),
                order.getItem().getProductId(),
                order.getItem().getQuantity(),
                java.time.OffsetDateTime.now()
        );
    }
}
