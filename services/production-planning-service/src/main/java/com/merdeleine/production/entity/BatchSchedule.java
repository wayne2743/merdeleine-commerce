package com.merdeleine.production.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "batch_schedule")
public class BatchSchedule {

    @Id
    @Column(name = "id", columnDefinition = "UUID")
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "batch_id", nullable = false, foreignKey = @ForeignKey(name = "fk_batch_schedule"))
    private Batch batch;

    @Column(name = "planned_production_date")
    private OffsetDateTime plannedProductionDate;

    @Column(name = "planned_ship_date")
    private OffsetDateTime plannedShipDate;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    // Constructors
    public BatchSchedule() {
    }

    public BatchSchedule(UUID id, OffsetDateTime plannedProductionDate, OffsetDateTime plannedShipDate, String notes) {
        this.id = id;
        this.plannedProductionDate = plannedProductionDate;
        this.plannedShipDate = plannedShipDate;
        this.notes = notes;
    }

    // Getters and Setters
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public Batch getBatch() {
        return batch;
    }

    public void setBatch(Batch batch) {
        this.batch = batch;
    }

    public OffsetDateTime getPlannedProductionDate() {
        return plannedProductionDate;
    }

    public void setPlannedProductionDate(OffsetDateTime plannedProductionDate) {
        this.plannedProductionDate = plannedProductionDate;
    }

    public OffsetDateTime getPlannedShipDate() {
        return plannedShipDate;
    }

    public void setPlannedShipDate(OffsetDateTime plannedShipDate) {
        this.plannedShipDate = plannedShipDate;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(OffsetDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
