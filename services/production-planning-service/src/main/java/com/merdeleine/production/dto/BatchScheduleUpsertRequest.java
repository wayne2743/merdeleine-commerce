package com.merdeleine.production.dto;

import java.time.OffsetDateTime;

public class BatchScheduleUpsertRequest {
    private OffsetDateTime plannedProductionDate;
    private OffsetDateTime plannedShipDate;
    private String notes;

    public OffsetDateTime getPlannedProductionDate() { return plannedProductionDate; }
    public void setPlannedProductionDate(OffsetDateTime plannedProductionDate) { this.plannedProductionDate = plannedProductionDate; }
    public OffsetDateTime getPlannedShipDate() { return plannedShipDate; }
    public void setPlannedShipDate(OffsetDateTime plannedShipDate) { this.plannedShipDate = plannedShipDate; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}
