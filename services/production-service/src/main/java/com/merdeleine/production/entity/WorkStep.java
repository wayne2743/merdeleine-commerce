package com.merdeleine.production.entity;

import com.merdeleine.production.enums.WorkStepStatus;
import jakarta.persistence.*;

import java.util.UUID;

@Entity
@Table(name = "work_step")
public class WorkStep {

    @Id
    @Column(name = "id", columnDefinition = "UUID")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "work_order_id", nullable = false, foreignKey = @ForeignKey(name = "fk_work_step"))
    private WorkOrder workOrder;

    @Column(name = "step_name", nullable = false, length = 100)
    private String stepName;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private WorkStepStatus status;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    // Constructors
    public WorkStep() {
    }

    public WorkStep(UUID id, String stepName, WorkStepStatus status) {
        this.id = id;
        this.stepName = stepName;
        this.status = status;
    }

    // Getters and Setters
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public WorkOrder getWorkOrder() {
        return workOrder;
    }

    public void setWorkOrder(WorkOrder workOrder) {
        this.workOrder = workOrder;
    }

    public String getStepName() {
        return stepName;
    }

    public void setStepName(String stepName) {
        this.stepName = stepName;
    }

    public WorkStepStatus getStatus() {
        return status;
    }

    public void setStatus(WorkStepStatus status) {
        this.status = status;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}
