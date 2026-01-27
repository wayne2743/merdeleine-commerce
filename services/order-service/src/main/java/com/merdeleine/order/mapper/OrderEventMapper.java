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
                order.getItems().stream().map(item -> new OrderEvent.Line(
                        item.getProductId(),
                        item.getVariantId(),
                        item.getQuantity()
                )).toList(),
                java.time.OffsetDateTime.now()
        );
    }
}
