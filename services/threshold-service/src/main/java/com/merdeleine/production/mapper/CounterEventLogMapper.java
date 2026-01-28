package com.merdeleine.production.mapper;

import com.merdeleine.messaging.OrderEvent;
import com.merdeleine.production.entity.CounterEventLog;

import java.util.UUID;

public class CounterEventLogMapper {

    public CounterEventLog toCounterEventLog(OrderEvent orderEvent) {
        return new CounterEventLog(
                UUID.randomUUID(),
                orderEvent.eventType(),
                orderEvent.eventId(),
                orderEvent.quantity()
        );
    }
}
