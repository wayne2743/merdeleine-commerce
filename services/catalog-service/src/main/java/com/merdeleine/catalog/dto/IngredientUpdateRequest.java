package com.merdeleine.catalog.dto;

import com.merdeleine.catalog.enums.IngredientAttribute;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public class IngredientUpdateRequest {

    @Size(max = 100, message = "name must not exceed 100 characters")
    private String name;

    @Min(value = 0, message = "unitPriceCents must be >= 0")
    private Integer unitPriceCents;

    @Size(max = 100, message = "brand must not exceed 100 characters")
    private String brand;

    @Size(max = 100, message = "origin must not exceed 100 characters")
    private String origin;

    @Size(max = 1000, message = "governmentRegistrationInfo must not exceed 1000 characters")
    private String governmentRegistrationInfo;

    private IngredientAttribute attribute;

    private LocalDate stockedAt;

    private LocalDate expiresAt;

    @DecimalMin(value = "0.000", inclusive = true, message = "stockQuantity must be >= 0")
    private BigDecimal stockQuantity;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getUnitPriceCents() {
        return unitPriceCents;
    }

    public void setUnitPriceCents(Integer unitPriceCents) {
        this.unitPriceCents = unitPriceCents;
    }

    public String getBrand() {
        return brand;
    }

    public void setBrand(String brand) {
        this.brand = brand;
    }

    public String getOrigin() {
        return origin;
    }

    public void setOrigin(String origin) {
        this.origin = origin;
    }

    public String getGovernmentRegistrationInfo() {
        return governmentRegistrationInfo;
    }

    public void setGovernmentRegistrationInfo(String governmentRegistrationInfo) {
        this.governmentRegistrationInfo = governmentRegistrationInfo;
    }

    public IngredientAttribute getAttribute() {
        return attribute;
    }

    public void setAttribute(IngredientAttribute attribute) {
        this.attribute = attribute;
    }

    public LocalDate getStockedAt() {
        return stockedAt;
    }

    public void setStockedAt(LocalDate stockedAt) {
        this.stockedAt = stockedAt;
    }

    public LocalDate getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(LocalDate expiresAt) {
        this.expiresAt = expiresAt;
    }

    public BigDecimal getStockQuantity() {
        return stockQuantity;
    }

    public void setStockQuantity(BigDecimal stockQuantity) {
        this.stockQuantity = stockQuantity;
    }
}

