package com.merdeleine.production.dto;

import com.merdeleine.production.enums.BatchStatus;
import jakarta.validation.constraints.Min;

import java.time.OffsetDateTime;

public class BatchUpdateRequest {
    @Min(0) private Integer targetQty;
    private BatchStatus status;
    private OffsetDateTime confirmedAt;

    public Integer getTargetQty() { return targetQty; }
    public void setTargetQty(Integer targetQty) { this.targetQty = targetQty; }
    public BatchStatus getStatus() { return status; }
    public void setStatus(BatchStatus status) { this.status = status; }
    public OffsetDateTime getConfirmedAt() { return confirmedAt; }
    public void setConfirmedAt(OffsetDateTime confirmedAt) { this.confirmedAt = confirmedAt; }
}
