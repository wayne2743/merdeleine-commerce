package com.merdeleine.production.dto;

import com.merdeleine.production.enums.BatchStatus;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.time.OffsetDateTime;
import java.util.UUID;

public class BatchCreateRequest {
    @NotNull private UUID sellWindowId;
    @NotNull private UUID productId;
    @NotNull @Min(0) private Integer targetQty;
    @NotNull private BatchStatus status;
    private OffsetDateTime confirmedAt;

    public UUID getSellWindowId() { return sellWindowId; }
    public void setSellWindowId(UUID sellWindowId) { this.sellWindowId = sellWindowId; }
    public UUID getProductId() { return productId; }
    public void setProductId(UUID productId) { this.productId = productId; }
    public Integer getTargetQty() { return targetQty; }
    public void setTargetQty(Integer targetQty) { this.targetQty = targetQty; }
    public BatchStatus getStatus() { return status; }
    public void setStatus(BatchStatus status) { this.status = status; }
    public OffsetDateTime getConfirmedAt() { return confirmedAt; }
    public void setConfirmedAt(OffsetDateTime confirmedAt) { this.confirmedAt = confirmedAt; }
}
