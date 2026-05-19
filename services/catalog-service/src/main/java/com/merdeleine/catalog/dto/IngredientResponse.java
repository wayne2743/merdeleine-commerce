package com.merdeleine.catalog.dto;

import com.merdeleine.catalog.entity.Ingredient;
import com.merdeleine.catalog.enums.IngredientAttribute;

import java.time.OffsetDateTime;
import java.util.UUID;

public record IngredientResponse(
        UUID id,
        String name,
        String brand,
        String origin,
        String governmentRegistrationInfo,
        IngredientAttribute attribute,
        Integer caloriesPer100g,
        String allergens,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
    public static IngredientResponse fromEntity(Ingredient ingredient) {
        return new IngredientResponse(
                ingredient.getId(),
                ingredient.getName(),
                ingredient.getBrand(),
                ingredient.getOrigin(),
                ingredient.getGovernmentRegistrationInfo(),
                ingredient.getAttribute(),
                ingredient.getCaloriesPer100g(),
                ingredient.getAllergens(),
                ingredient.getCreatedAt(),
                ingredient.getUpdatedAt()
        );
    }
}
