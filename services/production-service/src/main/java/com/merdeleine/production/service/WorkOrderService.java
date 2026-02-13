package com.merdeleine.production.service;

import com.merdeleine.messaging.BatchCreatedNotificationEvent;
import com.merdeleine.production.client.OrderServiceClient;
import com.merdeleine.production.dto.WorkOrderDtos.*;
import com.merdeleine.production.entity.WorkOrder;
import com.merdeleine.production.entity.WorkStep;
import com.merdeleine.production.enums.WorkOrderStatus;
import com.merdeleine.production.exception.NotFoundException;
import com.merdeleine.production.repository.WorkOrderRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class WorkOrderService {

    private final WorkOrderRepository workOrderRepository;
    private final OrderServiceClient orderServiceClient;

    public WorkOrderService(WorkOrderRepository workOrderRepository, OrderServiceClient orderServiceClient) {
        this.workOrderRepository = workOrderRepository;
        this.orderServiceClient = orderServiceClient;
    }

    @Transactional
    public WorkOrderResponse create(CreateWorkOrderRequest req) {
        WorkOrder wo = new WorkOrder();
        wo.setId(UUID.randomUUID());
        wo.setBatchId(req.batchId());
        wo.setStatus(req.status());
        wo.setStartAt(req.startAt());
        wo.setEndAt(req.endAt());
        wo.setOperator(req.operator());

        for (WorkStepUpsertRequest s : req.steps()) {
            WorkStep step = new WorkStep();
            step.setId((s.id() != null) ? s.id() : UUID.randomUUID());
            step.setStepName(s.stepName());
            step.setStatus(s.status());
            step.setNotes(s.notes());
            wo.addStep(step);
        }

        WorkOrder saved = workOrderRepository.save(wo);
        return toResponse(saved);
    }

    @Transactional
    public WorkOrderResponse get(UUID id) {
        WorkOrder wo = workOrderRepository.findWithStepsById(id)
                .orElseThrow(() -> new NotFoundException("WorkOrder not found: " + id));
        return toResponse(wo);
    }

    @Transactional
    public List<WorkOrderResponse> list(Optional<UUID> batchId) {
        List<WorkOrder> list = batchId.map(workOrderRepository::findByBatchId)
                .orElseGet(workOrderRepository::findAll);

        // 若你希望 list 也帶 steps，可改成自訂 query + EntityGraph；這裡先不強制
        return list.stream()
                .map(this::toResponseWithoutSteps) // 避免 list 太重
                .toList();
    }

    @Transactional
    public WorkOrderResponse update(UUID id, UpdateWorkOrderRequest req) {
        WorkOrder wo = workOrderRepository.findWithStepsById(id)
                .orElseThrow(() -> new NotFoundException("WorkOrder not found: " + id));

        wo.setStatus(req.status());
        wo.setStartAt(req.startAt());
        wo.setEndAt(req.endAt());
        wo.setOperator(req.operator());

        // --- 同步 steps：以 req.steps 為準 ---
        Map<UUID, WorkStep> existing = wo.getSteps().stream()
                .collect(Collectors.toMap(WorkStep::getId, it -> it));

        Set<UUID> incomingIds = new HashSet<>();

        for (WorkStepUpsertRequest s : req.steps()) {
            UUID stepId = (s.id() != null) ? s.id() : UUID.randomUUID();
            incomingIds.add(stepId);

            WorkStep step = existing.get(stepId);
            if (step == null) {
                step = new WorkStep();
                step.setId(stepId);
                step.setStepName(s.stepName());
                step.setStatus(s.status());
                step.setNotes(s.notes());
                wo.addStep(step);
            } else {
                step.setStepName(s.stepName());
                step.setStatus(s.status());
                step.setNotes(s.notes());
            }
        }

        // remove orphan (request 沒帶到的就刪)
        List<WorkStep> toRemove = wo.getSteps().stream()
                .filter(st -> !incomingIds.contains(st.getId()))
                .toList();
        toRemove.forEach(wo::removeStep);

        WorkOrder saved = workOrderRepository.save(wo);
        return toResponse(saved);
    }

    @Transactional
    public void delete(UUID id) {
        if (!workOrderRepository.existsById(id)) {
            throw new NotFoundException("WorkOrder not found: " + id);
        }
        workOrderRepository.deleteById(id); // cascade + orphanRemoval 會刪 steps
    }

    @Transactional
    public void startProduction(BatchCreatedNotificationEvent event) {

        // 1) 先關單（強一致 gate）
        OrderServiceClient.CloseQuotaResponse resp = orderServiceClient.closeQuota(
                event.sellWindowId(),
                event.productId(),
                event.eventId(),
                "PRODUCTION_STARTED"
        );

        // 若你希望「已經關過也算成功」：resp.closed=false 也可以繼續
        // 若你希望一定要由這次呼叫關成功才行：就檢查 resp.closed == true
        if (!"CLOSED".equals(resp.status())) {
            throw new IllegalStateException("close quota failed");
        }

        // 2) 再把 WorkOrder 改成 IN_PROGRESS（或新建一張 IN_PROGRESS）
        WorkOrder wo = new WorkOrder();
        wo.setId(UUID.randomUUID());
        wo.setBatchId(event.batchId());
        wo.setStatus(WorkOrderStatus.IN_PROGRESS);
        wo.setStartAt(OffsetDateTime.now());
        workOrderRepository.save(wo);
    }


    // ----------------- mapping -----------------

    private WorkOrderResponse toResponse(WorkOrder wo) {
        List<WorkStepResponse> steps = wo.getSteps().stream()
                .map(s -> new WorkStepResponse(s.getId(), s.getStepName(), s.getStatus(), s.getNotes()))
                .toList();

        return new WorkOrderResponse(
                wo.getId(),
                wo.getBatchId(),
                wo.getStatus(),
                wo.getStartAt(),
                wo.getEndAt(),
                wo.getOperator(),
                steps
        );
    }

    private WorkOrderResponse toResponseWithoutSteps(WorkOrder wo) {
        return new WorkOrderResponse(
                wo.getId(),
                wo.getBatchId(),
                wo.getStatus(),
                wo.getStartAt(),
                wo.getEndAt(),
                wo.getOperator(),
                List.of()
        );
    }
}
