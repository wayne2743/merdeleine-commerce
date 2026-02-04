package com.merdeleine.order.mapper;

import com.merdeleine.order.entity.Order;
import com.merdeleine.messaging.OrderAccumulatedEvent;

public class OrderEventMapper {

    public OrderAccumulatedEvent toOrderEvent(Order order) {
        return new OrderAccumulatedEvent(
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
