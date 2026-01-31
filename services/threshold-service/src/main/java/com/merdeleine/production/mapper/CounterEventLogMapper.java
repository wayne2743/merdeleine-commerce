package com.merdeleine.production.mapper;

import com.merdeleine.messaging.OrderAccumulatedEvent;
import com.merdeleine.production.entity.BatchCounter;
import com.merdeleine.production.entity.CounterEventLog;

import java.util.UUID;

public class CounterEventLogMapper {

    public CounterEventLog toCounterEventLog(OrderAccumulatedEvent orderAccumulatedEvent, BatchCounter batchCounter) {
        return new CounterEventLog(
                UUID.randomUUID(),
                batchCounter,
                orderAccumulatedEvent.eventType(),
                orderAccumulatedEvent.eventId(),
                orderAccumulatedEvent.quantity()
        );
    }
}
