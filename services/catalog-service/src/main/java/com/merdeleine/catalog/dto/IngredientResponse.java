package com.merdeleine.catalog.dto;

import com.merdeleine.catalog.entity.Ingredient;
import com.merdeleine.catalog.enums.IngredientAttribute;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

public record IngredientResponse(
        UUID id,
        String name,
        Integer unitPriceCents,
        String brand,
        String origin,
        String governmentRegistrationInfo,
        IngredientAttribute attribute,
        LocalDate stockedAt,
        LocalDate expiresAt,
        Integer caloriesPer100g,
        String allergens,
        BigDecimal stockQuantity,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
    public static IngredientResponse fromEntity(Ingredient ingredient) {
        return new IngredientResponse(
                ingredient.getId(),
                ingredient.getName(),
                ingredient.getUnitPriceCents(),
                ingredient.getBrand(),
                ingredient.getOrigin(),
                ingredient.getGovernmentRegistrationInfo(),
                ingredient.getAttribute(),
                ingredient.getStockedAt(),
                ingredient.getExpiresAt(),
                ingredient.getCaloriesPer100g(),
                ingredient.getAllergens(),
                ingredient.getStockQuantity(),
                ingredient.getCreatedAt(),
                ingredient.getUpdatedAt()
        );
    }
}
