package com.merdeleine.catalog.dto;

import com.merdeleine.catalog.entity.Product;
import com.merdeleine.catalog.enums.ProductStatus;

import java.time.OffsetDateTime;
import java.util.UUID;

public class ProductResponse {
    
    private UUID id;
    private String name;
    private String description;
    private ProductStatus status;
    private Integer unitPriceCents;
    private String currency;
    private Integer defaultMinQty;
    private Integer defaultMaxQty;
    private Integer defaultLeadDays;
    private Integer defaultShipDays;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

    public ProductResponse() {
    }

    public ProductResponse(UUID id, String name, String description, ProductStatus status, 
                          Integer unitPriceCents, String currency,
                          Integer defaultMinQty, Integer defaultMaxQty,
                          Integer defaultLeadDays, Integer defaultShipDays,
                          OffsetDateTime createdAt, OffsetDateTime updatedAt) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.status = status;
        this.unitPriceCents = unitPriceCents;
        this.currency = currency;
        this.defaultMinQty = defaultMinQty;
        this.defaultMaxQty = defaultMaxQty;
        this.defaultLeadDays = defaultLeadDays;
        this.defaultShipDays = defaultShipDays;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static ProductResponse fromEntity(Product product) {
        return new ProductResponse(
            product.getId(),
            product.getName(),
            product.getDescription(),
            product.getStatus(),
            product.getUnitPriceCents(),
            product.getCurrency(),
            product.getDefaultMinQty(),
            product.getDefaultMaxQty(),
            product.getDefaultLeadDays(),
            product.getDefaultShipDays(),
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

    public Integer getUnitPriceCents() {
        return unitPriceCents;
    }

    public void setUnitPriceCents(Integer unitPriceCents) {
        this.unitPriceCents = unitPriceCents;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public Integer getDefaultMinQty() {
        return defaultMinQty;
    }

    public void setDefaultMinQty(Integer defaultMinQty) {
        this.defaultMinQty = defaultMinQty;
    }

    public Integer getDefaultMaxQty() {
        return defaultMaxQty;
    }

    public void setDefaultMaxQty(Integer defaultMaxQty) {
        this.defaultMaxQty = defaultMaxQty;
    }

    public Integer getDefaultLeadDays() {
        return defaultLeadDays;
    }

    public void setDefaultLeadDays(Integer defaultLeadDays) {
        this.defaultLeadDays = defaultLeadDays;
    }

    public Integer getDefaultShipDays() {
        return defaultShipDays;
    }

    public void setDefaultShipDays(Integer defaultShipDays) {
        this.defaultShipDays = defaultShipDays;
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
