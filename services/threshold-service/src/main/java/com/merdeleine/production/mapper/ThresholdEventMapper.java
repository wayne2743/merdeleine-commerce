package com.merdeleine.production.mapper;

import com.merdeleine.messaging.ThresholdReachedEvent;
import com.merdeleine.production.entity.BatchCounter;

import java.util.UUID;

public class ThresholdEventMapper {

    public ThresholdReachedEvent toThresholdReachedEvent(BatchCounter batchCounter, String eventType) {
        return new ThresholdReachedEvent(
                UUID.randomUUID(),
                eventType,
                batchCounter.getProductId(),
                batchCounter.getSellWindowId(),
                batchCounter.getId(),
                batchCounter.getPaidQty(),
                batchCounter.getThresholdQty()
        );
    }
}
