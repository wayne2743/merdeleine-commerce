package com.merdeleine.production.planning.mapper;


import com.merdeleine.messaging.BatchCreatedNotificationEvent;
import com.merdeleine.messaging.ThresholdReachedEvent;
import com.merdeleine.production.planning.dto.BatchResponse;
import com.merdeleine.production.planning.dto.BatchScheduleResponse;
import com.merdeleine.production.planning.entity.Batch;
import com.merdeleine.production.planning.entity.BatchSchedule;
import com.merdeleine.production.planning.enums.BatchStatus;

import java.util.UUID;

public final class BatchMappers {
    private BatchMappers() {}

    public static BatchResponse toBatchResponse(Batch b) {
        BatchResponse r = new BatchResponse();
        r.setId(b.getId());
        r.setSellWindowId(b.getSellWindowId());
        r.setProductId(b.getProductId());
        r.setTargetQty(b.getTargetQty());
        r.setStatus(b.getStatus());
        r.setCreatedAt(b.getCreatedAt());
        r.setConfirmedAt(b.getConfirmedAt());
        r.setHasSchedule(b.getSchedule() != null);
        return r;
    }


    public static BatchScheduleResponse toScheduleResponse(BatchSchedule s) {
        BatchScheduleResponse r = new BatchScheduleResponse();
        r.setId(s.getId());
        r.setBatchId(s.getBatch().getId());
        r.setPlannedProductionDate(s.getPlannedProductionDate());
        r.setPlannedShipDate(s.getPlannedShipDate());
        r.setNotes(s.getNotes());
        r.setUpdatedAt(s.getUpdatedAt());
        return r;
    }

    public static Batch toBatch(ThresholdReachedEvent event, BatchStatus batchStatus, UUID batchId) {
        return new Batch(
                batchId,
                event.sellWindowId(),
                event.productId(),
                event.targetQuantity(),
                batchStatus
        );
    }

    public static Object toBatchCreatedNotificationEvent(ThresholdReachedEvent event, Batch batch) {
        return new BatchCreatedNotificationEvent(
                event.eventId(),
                "batch.created.notification.v1",
                batch.getId(),
                event.productId(),
                event.sellWindowId(),
                event.totalQuantity(),
                event.targetQuantity()
        );
    }
}
