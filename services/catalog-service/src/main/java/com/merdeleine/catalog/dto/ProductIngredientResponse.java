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
        Integer caloriesPer100g,
        String allergens,
        BigDecimal requiredAmount,
        IngredientUnit unit
) {
    public static ProductIngredientResponse fromEntity(ProductIngredient entity) {
        return new ProductIngredientResponse(
                entity.getIngredient().getId(),
                entity.getIngredient().getName(),
                entity.getIngredient().getAttribute(),
                entity.getIngredient().getCaloriesPer100g(),
                entity.getIngredient().getAllergens(),
                entity.getRequiredAmount(),
                entity.getUnit()
        );
    }
}
