package com.merdeleine.catalog.dto;

import com.merdeleine.catalog.enums.IngredientUnit;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

public class ProductIngredientRequest {

    @NotNull(message = "ingredientId is required")
    private UUID ingredientId;

    @NotNull(message = "requiredAmount is required")
    @DecimalMin(value = "0.000", inclusive = false, message = "requiredAmount must be > 0")
    private BigDecimal requiredAmount;

    @NotNull(message = "unit is required")
    private IngredientUnit unit;

    public UUID getIngredientId() {
        return ingredientId;
    }

    public void setIngredientId(UUID ingredientId) {
        this.ingredientId = ingredientId;
    }

    public BigDecimal getRequiredAmount() {
        return requiredAmount;
    }

    public void setRequiredAmount(BigDecimal requiredAmount) {
        this.requiredAmount = requiredAmount;
    }

    public IngredientUnit getUnit() {
        return unit;
    }

    public void setUnit(IngredientUnit unit) {
        this.unit = unit;
    }
}

