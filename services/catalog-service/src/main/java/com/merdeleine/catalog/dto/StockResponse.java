package com.merdeleine.catalog.dto;

import com.merdeleine.catalog.entity.Stock;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

public record StockResponse(
        UUID id,
        UUID ingredientId,
        String ingredientName,
        Integer unitPriceCents,
        LocalDate stockedAt,
        LocalDate expiresAt,
        BigDecimal stockQuantity,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
    public static StockResponse fromEntity(Stock stock) {
        return new StockResponse(
                stock.getId(),
                stock.getIngredient().getId(),
                stock.getIngredient().getName(),
                stock.getUnitPriceCents(),
                stock.getStockedAt(),
                stock.getExpiresAt(),
                stock.getStockQuantity(),
                stock.getCreatedAt(),
                stock.getUpdatedAt()
        );
    }
}
