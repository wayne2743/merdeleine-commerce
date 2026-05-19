package com.merdeleine.catalog.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;

import java.math.BigDecimal;
import java.time.LocalDate;

public class StockUpdateRequest {

    @Min(value = 0, message = "unitPriceCents must be >= 0")
    private Integer unitPriceCents;

    private LocalDate stockedAt;

    private LocalDate expiresAt;

    @DecimalMin(value = "0.000", inclusive = true, message = "stockQuantity must be >= 0")
    private BigDecimal stockQuantity;

    public Integer getUnitPriceCents() { return unitPriceCents; }
    public void setUnitPriceCents(Integer unitPriceCents) { this.unitPriceCents = unitPriceCents; }

    public LocalDate getStockedAt() { return stockedAt; }
    public void setStockedAt(LocalDate stockedAt) { this.stockedAt = stockedAt; }

    public LocalDate getExpiresAt() { return expiresAt; }
    public void setExpiresAt(LocalDate expiresAt) { this.expiresAt = expiresAt; }

    public BigDecimal getStockQuantity() { return stockQuantity; }
    public void setStockQuantity(BigDecimal stockQuantity) { this.stockQuantity = stockQuantity; }
}
