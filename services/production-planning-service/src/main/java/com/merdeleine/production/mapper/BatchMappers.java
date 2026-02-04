package com.merdeleine.production.mapper;


import com.merdeleine.messaging.BatchCreatedNotificationEvent;
import com.merdeleine.messaging.ThresholdReachedEvent;
import com.merdeleine.production.dto.BatchOrderLinkResponse;
import com.merdeleine.production.dto.BatchResponse;
import com.merdeleine.production.dto.BatchScheduleResponse;
import com.merdeleine.production.entity.Batch;
import com.merdeleine.production.entity.BatchOrderLink;
import com.merdeleine.production.entity.BatchSchedule;
import com.merdeleine.production.enums.BatchStatus;

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
        r.setOrderLinkCount(b.getOrderLinks() == null ? 0 : b.getOrderLinks().size());
        r.setHasSchedule(b.getSchedule() != null);
        return r;
    }

    public static BatchOrderLinkResponse toOrderLinkResponse(BatchOrderLink l) {
        BatchOrderLinkResponse r = new BatchOrderLinkResponse();
        r.setId(l.getId());
        r.setBatchId(l.getBatch().getId());
        r.setOrderId(l.getOrderId());
        r.setQuantity(l.getQuantity());
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

    public static Batch toBatch(ThresholdReachedEvent event, BatchStatus batchStatus) {
        return new Batch(
                null,
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
                event.totalQuantity()
        );
    }
}
