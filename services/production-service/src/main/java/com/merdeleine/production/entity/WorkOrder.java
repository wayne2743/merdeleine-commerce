package com.merdeleine.production.entity;

import com.merdeleine.production.enums.WorkOrderStatus;
import jakarta.persistence.*;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "work_order")
public class WorkOrder {

    @Id
    @Column(name = "id", columnDefinition = "UUID")
    private UUID id;

    @Column(name = "batch_id", nullable = false, columnDefinition = "UUID")
    private UUID batchId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private WorkOrderStatus status;

    @Column(name = "start_at")
    private OffsetDateTime startAt;

    @Column(name = "end_at")
    private OffsetDateTime endAt;

    @Column(name = "operator", length = 100)
    private String operator;

    @OneToMany(mappedBy = "workOrder", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<WorkStep> steps = new ArrayList<>();

    // Constructors
    public WorkOrder() {
    }

    public WorkOrder(UUID id, UUID batchId, WorkOrderStatus status) {
        this.id = id;
        this.batchId = batchId;
        this.status = status;
    }

    // Getters and Setters
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getBatchId() {
        return batchId;
    }

    public void setBatchId(UUID batchId) {
        this.batchId = batchId;
    }

    public WorkOrderStatus getStatus() {
        return status;
    }

    public void setStatus(WorkOrderStatus status) {
        this.status = status;
    }

    public OffsetDateTime getStartAt() {
        return startAt;
    }

    public void setStartAt(OffsetDateTime startAt) {
        this.startAt = startAt;
    }

    public OffsetDateTime getEndAt() {
        return endAt;
    }

    public void setEndAt(OffsetDateTime endAt) {
        this.endAt = endAt;
    }

    public String getOperator() {
        return operator;
    }

    public void setOperator(String operator) {
        this.operator = operator;
    }

    public List<WorkStep> getSteps() {
        return steps;
    }

    public void setSteps(List<WorkStep> steps) {
        this.steps = steps;
    }

    public void addStep(WorkStep step) {
        steps.add(step);
        step.setWorkOrder(this);
    }

    public void removeStep(WorkStep step) {
        steps.remove(step);
        step.setWorkOrder(null);
    }
}
