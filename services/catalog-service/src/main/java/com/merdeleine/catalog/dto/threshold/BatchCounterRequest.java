package com.merdeleine.catalog.dto.threshold;

import java.util.UUID;

public class BatchCounterRequest {

    private UUID sellWindowId;
    private UUID productId;
    private Integer thresholdQty;
    private String status; // 用 String，避免 enum 耦合

    public BatchCounterRequest() {}

    public UUID getSellWindowId() {
        return sellWindowId;
    }

    public void setSellWindowId(UUID sellWindowId) {
        this.sellWindowId = sellWindowId;
    }

    public UUID getProductId() {
        return productId;
    }

    public void setProductId(UUID productId) {
        this.productId = productId;
    }

    public Integer getThresholdQty() {
        return thresholdQty;
    }

    public void setThresholdQty(Integer thresholdQty) {
        this.thresholdQty = thresholdQty;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
