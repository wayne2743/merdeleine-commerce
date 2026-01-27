package com.merdeleine.catalog.dto.threshold;

import java.time.OffsetDateTime;
import java.util.UUID;

public class BatchCounterResponse {

    private UUID id;
    private UUID sellWindowId;
    private UUID productId;
    private Integer paidQty;
    private Integer thresholdQty;
    private String status;
    private OffsetDateTime reachedAt;
    private OffsetDateTime updatedAt;

    public BatchCounterResponse() {}

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getSellWindowId() { return sellWindowId; }
    public void setSellWindowId(UUID sellWindowId) { this.sellWindowId = sellWindowId; }

    public UUID getProductId() { return productId; }
    public void setProductId(UUID productId) { this.productId = productId; }

    public Integer getPaidQty() { return paidQty; }
    public void setPaidQty(Integer paidQty) { this.paidQty = paidQty; }

    public Integer getThresholdQty() { return thresholdQty; }
    public void setThresholdQty(Integer thresholdQty) { this.thresholdQty = thresholdQty; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public OffsetDateTime getReachedAt() { return reachedAt; }
    public void setReachedAt(OffsetDateTime reachedAt) { this.reachedAt = reachedAt; }

    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(OffsetDateTime updatedAt) { this.updatedAt = updatedAt; }
}
