package com.merdeleine.production.dto;

import jakarta.validation.constraints.Min;

public class BatchOrderLinkUpdateRequest {
    @Min(1) private Integer quantity;

    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }
}
