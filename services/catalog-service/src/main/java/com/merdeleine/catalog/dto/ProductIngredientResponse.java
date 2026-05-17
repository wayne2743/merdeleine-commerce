package com.merdeleine.catalog.dto;

import com.merdeleine.catalog.entity.ProductIngredient;
import com.merdeleine.catalog.enums.IngredientAttribute;
import com.merdeleine.catalog.enums.IngredientUnit;

import java.math.BigDecimal;
import java.util.UUID;

public record ProductIngredientResponse(
        UUID ingredientId,
        String ingredientName,
        IngredientAttribute ingredientAttribute,
        BigDecimal requiredAmount,
        IngredientUnit unit
) {
    public static ProductIngredientResponse fromEntity(ProductIngredient entity) {
        return new ProductIngredientResponse(
                entity.getIngredient().getId(),
                entity.getIngredient().getName(),
                entity.getIngredient().getAttribute(),
                entity.getRequiredAmount(),
                entity.getUnit()
        );
    }
}

