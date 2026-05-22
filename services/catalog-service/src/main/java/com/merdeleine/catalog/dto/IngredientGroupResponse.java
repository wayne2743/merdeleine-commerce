package com.merdeleine.catalog.dto;

import com.merdeleine.catalog.entity.IngredientGroup;

import java.time.OffsetDateTime;
import java.util.UUID;

public record IngredientGroupResponse(
        UUID id,
        String name,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
    public static IngredientGroupResponse fromEntity(IngredientGroup entity) {
        return new IngredientGroupResponse(
                entity.getId(),
                entity.getName(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
