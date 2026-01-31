package com.merdeleine.production.mapper;

import com.merdeleine.messaging.ThresholdReachedEvent;
import com.merdeleine.production.entity.BatchCounter;

import java.util.UUID;

public class ThresholdEventMapper {

    public ThresholdReachedEvent toThresholdReachedEvent(BatchCounter batchCounter) {
        return new ThresholdReachedEvent(
                UUID.randomUUID(),
                "threshold.reached.v1",
                batchCounter.getProductId(),
                batchCounter.getSellWindowId(),
                batchCounter.getPaidQty()
        );
    }
}
