package com.merdeleine.catalog.dto;

import com.merdeleine.catalog.domain.ProductStatus;
import com.merdeleine.catalog.entity.Product;

import java.time.OffsetDateTime;
import java.util.UUID;

public class ProductResponse {
    
    private UUID id;
    private String name;
    private String description;
    private ProductStatus status;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

    public ProductResponse() {
    }

    public ProductResponse(UUID id, String name, String description, ProductStatus status, 
                          OffsetDateTime createdAt, OffsetDateTime updatedAt) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static ProductResponse fromEntity(Product product) {
        return new ProductResponse(
            product.getId(),
            product.getName(),
            product.getDescription(),
            product.getStatus(),
            product.getCreatedAt(),
            product.getUpdatedAt()
        );
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public ProductStatus getStatus() {
        return status;
    }

    public void setStatus(ProductStatus status) {
        this.status = status;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(OffsetDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
