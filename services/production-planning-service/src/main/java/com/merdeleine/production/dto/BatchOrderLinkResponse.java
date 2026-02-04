package com.merdeleine.production.dto;

import java.util.UUID;

public class BatchOrderLinkResponse {
    private UUID id;
    private UUID batchId;
    private UUID orderId;
    private Integer quantity;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getBatchId() { return batchId; }
    public void setBatchId(UUID batchId) { this.batchId = batchId; }
    public UUID getOrderId() { return orderId; }
    public void setOrderId(UUID orderId) { this.orderId = orderId; }
    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }
}
