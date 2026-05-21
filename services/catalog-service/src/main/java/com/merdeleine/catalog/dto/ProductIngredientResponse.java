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
        IngredientUnit unit,
        UUID ingredientGroupId,
        String ingredientGroupName
) {
    public static ProductIngredientResponse fromEntity(ProductIngredient entity) {
        UUID groupId = entity.getIngredientGroup() != null ? entity.getIngredientGroup().getId() : null;
        String groupName = entity.getIngredientGroup() != null ? entity.getIngredientGroup().getName() : null;
        return new ProductIngredientResponse(
                entity.getIngredient().getId(),
                entity.getIngredient().getName(),
                entity.getIngredient().getAttribute(),
                entity.getIngredient().getCaloriesPer100g(),
                entity.getIngredient().getAllergens(),
                entity.getRequiredAmount(),
                entity.getUnit(),
                groupId,
                groupName
        );
    }
}
