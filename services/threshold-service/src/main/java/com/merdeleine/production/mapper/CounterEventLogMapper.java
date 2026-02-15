package com.merdeleine.production.mapper;

import com.merdeleine.messaging.OrderReservedEvent;
import com.merdeleine.production.entity.BatchCounter;
import com.merdeleine.production.entity.CounterEventLog;

import java.util.UUID;

public class CounterEventLogMapper {

    public CounterEventLog toCounterEventLog(OrderReservedEvent orderReservedEvent, BatchCounter batchCounter) {
        return new CounterEventLog(
                UUID.randomUUID(),
                batchCounter,
                orderReservedEvent.eventType(),
                orderReservedEvent.eventId(),
                orderReservedEvent.quantity()
        );
    }
}
