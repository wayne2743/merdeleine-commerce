package com.merdeleine.production.dto;

import com.merdeleine.production.enums.BatchStatus;

import java.time.OffsetDateTime;
import java.util.UUID;

public class BatchResponse {
    private UUID id;
    private UUID sellWindowId;
    private UUID productId;
    private Integer targetQty;
    private BatchStatus status;
    private OffsetDateTime createdAt;
    private OffsetDateTime confirmedAt;

    // summary fields
    private int orderLinkCount;
    private boolean hasSchedule;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getSellWindowId() { return sellWindowId; }
    public void setSellWindowId(UUID sellWindowId) { this.sellWindowId = sellWindowId; }
    public UUID getProductId() { return productId; }
    public void setProductId(UUID productId) { this.productId = productId; }
    public Integer getTargetQty() { return targetQty; }
    public void setTargetQty(Integer targetQty) { this.targetQty = targetQty; }
    public BatchStatus getStatus() { return status; }
    public void setStatus(BatchStatus status) { this.status = status; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
    public OffsetDateTime getConfirmedAt() { return confirmedAt; }
    public void setConfirmedAt(OffsetDateTime confirmedAt) { this.confirmedAt = confirmedAt; }
    public int getOrderLinkCount() { return orderLinkCount; }
    public void setOrderLinkCount(int orderLinkCount) { this.orderLinkCount = orderLinkCount; }
    public boolean isHasSchedule() { return hasSchedule; }
    public void setHasSchedule(boolean hasSchedule) { this.hasSchedule = hasSchedule; }
}
