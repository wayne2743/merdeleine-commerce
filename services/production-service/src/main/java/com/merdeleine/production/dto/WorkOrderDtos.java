package com.merdeleine.production.dto;

import com.merdeleine.production.enums.WorkOrderStatus;
import com.merdeleine.production.enums.WorkStepStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class WorkOrderDtos {

    public record WorkStepUpsertRequest(
            UUID id, // update 才需要；create 可為 null
            @NotBlank String stepName,
            @NotNull WorkStepStatus status,
            String notes
    ) {}

    public record CreateWorkOrderRequest(
            @NotNull UUID batchId,
            @NotNull WorkOrderStatus status,
            OffsetDateTime startAt,
            OffsetDateTime endAt,
            String operator,
            @Valid List<WorkStepUpsertRequest> steps
    ) {
        public CreateWorkOrderRequest {
            steps = (steps == null) ? new ArrayList<>() : steps;
        }
    }

    public record UpdateWorkOrderRequest(
            @NotNull WorkOrderStatus status,
            OffsetDateTime startAt,
            OffsetDateTime endAt,
            String operator,
            @Valid List<WorkStepUpsertRequest> steps // 以此清單為準做同步（含刪除 orphan）
    ) {
        public UpdateWorkOrderRequest {
            steps = (steps == null) ? new ArrayList<>() : steps;
        }
    }

    public record WorkStepResponse(
            UUID id,
            String stepName,
            WorkStepStatus status,
            String notes
    ) {}

    public record WorkOrderResponse(
            UUID id,
            UUID batchId,
            WorkOrderStatus status,
            OffsetDateTime startAt,
            OffsetDateTime endAt,
            String operator,
            List<WorkStepResponse> steps
    ) {}
}
