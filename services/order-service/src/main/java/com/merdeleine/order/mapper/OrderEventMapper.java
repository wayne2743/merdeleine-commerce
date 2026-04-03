package com.merdeleine.order.mapper;

import com.merdeleine.order.entity.Order;
import com.merdeleine.messaging.OrderCancelledEvent;
import com.merdeleine.messaging.OrderReservedEvent;

public class OrderEventMapper {

    /** 使用 order item 的實際總數量（新建單時使用）*/
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

    /** 使用指定的 delta quantity（合併加量時只發本次新增量給 threshold）*/
    public OrderReservedEvent toOrderEvent(Order order, String eventType, int deltaQty) {
        return new OrderReservedEvent(
                java.util.UUID.randomUUID(),
                eventType,
                order.getId(),
                order.getSellWindowId(),
                order.getItem().getProductId(),
                deltaQty,
                java.time.OffsetDateTime.now()
        );
    }

    public OrderCancelledEvent toOrderCancelledEvent(Order order, String eventType) {
        return new OrderCancelledEvent(
                java.util.UUID.randomUUID(),
                eventType,
                order.getId(),
                order.getSellWindowId(),
                order.getItem().getProductId(),
                order.getItem().getQuantity(),
                java.time.OffsetDateTime.now()
        );
    }

    public OrderCancelledEvent toOrderCancelledEvent(Order order, String eventType, int deltaQty) {
        return new OrderCancelledEvent(
                java.util.UUID.randomUUID(),
                eventType,
                order.getId(),
                order.getSellWindowId(),
                order.getItem().getProductId(),
                deltaQty,
                java.time.OffsetDateTime.now()
        );
    }
}
