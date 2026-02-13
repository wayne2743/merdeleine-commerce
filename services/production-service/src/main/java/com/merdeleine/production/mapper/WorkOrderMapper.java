package com.merdeleine.production.mapper;

import com.merdeleine.messaging.BatchCreatedNotificationEvent;
import com.merdeleine.production.entity.WorkOrder;
import com.merdeleine.production.enums.WorkOrderStatus;

import java.time.OffsetDateTime;

public class WorkOrderMapper {
    public static WorkOrder toWorkOrder(BatchCreatedNotificationEvent event, WorkOrderStatus workOrderStatus, OffsetDateTime startAt, OffsetDateTime endAt) {
        WorkOrder workOrder = new WorkOrder();
        workOrder.setId(event.batchId());
        workOrder.setBatchId(event.batchId());
        workOrder.setStatus(workOrderStatus);
        workOrder.setStartAt(startAt);
        workOrder.setEndAt(endAt);
        return workOrder;
    }
}
