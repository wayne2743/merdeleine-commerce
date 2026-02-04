package com.merdeleine.production.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public class BatchScheduleResponse {
    private UUID id;
    private UUID batchId;
    private OffsetDateTime plannedProductionDate;
    private OffsetDateTime plannedShipDate;
    private String notes;
    private OffsetDateTime updatedAt;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getBatchId() { return batchId; }
    public void setBatchId(UUID batchId) { this.batchId = batchId; }
    public OffsetDateTime getPlannedProductionDate() { return plannedProductionDate; }
    public void setPlannedProductionDate(OffsetDateTime plannedProductionDate) { this.plannedProductionDate = plannedProductionDate; }
    public OffsetDateTime getPlannedShipDate() { return plannedShipDate; }
    public void setPlannedShipDate(OffsetDateTime plannedShipDate) { this.plannedShipDate = plannedShipDate; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(OffsetDateTime updatedAt) { this.updatedAt = updatedAt; }
}
