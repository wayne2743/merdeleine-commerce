package com.merdeleine.catalog.entity;

import com.merdeleine.catalog.enums.IngredientAttribute;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "ingredient")
public class Ingredient {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(name = "unit_price_cents", nullable = false)
    private Integer unitPriceCents;

    @Column(length = 100)
    private String brand;

    @Column(length = 100)
    private String origin;

    @Column(name = "government_registration_info", length = 1000)
    private String governmentRegistrationInfo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private IngredientAttribute attribute;

    @Column(name = "stocked_at")
    private LocalDate stockedAt;

    @Column(name = "expires_at")
    private LocalDate expiresAt;

    @Column(name = "calories_per_100g")
    private Integer caloriesPer100g;

    @Column(length = 500)
    private String allergens;

    @Column(name = "stock_quantity", nullable = false, precision = 14, scale = 3)
    private BigDecimal stockQuantity;

    @Column(nullable = false)
    private OffsetDateTime createdAt;

    @Column(nullable = false)
    private OffsetDateTime updatedAt;

    @PrePersist
    void prePersist() {
        if (id == null) id = UUID.randomUUID();
        if (stockQuantity == null) stockQuantity = BigDecimal.ZERO;
        createdAt = OffsetDateTime.now();
        updatedAt = createdAt;
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = OffsetDateTime.now();
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

    public Integer getUnitPriceCents() {
        return unitPriceCents;
    }

    public void setUnitPriceCents(Integer unitPriceCents) {
        this.unitPriceCents = unitPriceCents;
    }

    public String getBrand() {
        return brand;
    }

    public void setBrand(String brand) {
        this.brand = brand;
    }

    public String getOrigin() {
        return origin;
    }

    public void setOrigin(String origin) {
        this.origin = origin;
    }

    public String getGovernmentRegistrationInfo() {
        return governmentRegistrationInfo;
    }

    public void setGovernmentRegistrationInfo(String governmentRegistrationInfo) {
        this.governmentRegistrationInfo = governmentRegistrationInfo;
    }

    public IngredientAttribute getAttribute() {
        return attribute;
    }

    public void setAttribute(IngredientAttribute attribute) {
        this.attribute = attribute;
    }

    public LocalDate getStockedAt() {
        return stockedAt;
    }

    public void setStockedAt(LocalDate stockedAt) {
        this.stockedAt = stockedAt;
    }

    public LocalDate getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(LocalDate expiresAt) {
        this.expiresAt = expiresAt;
    }

    public Integer getCaloriesPer100g() {
        return caloriesPer100g;
    }

    public void setCaloriesPer100g(Integer caloriesPer100g) {
        this.caloriesPer100g = caloriesPer100g;
    }

    public String getAllergens() {
        return allergens;
    }

    public void setAllergens(String allergens) {
        this.allergens = allergens;
    }

    public BigDecimal getStockQuantity() {
        return stockQuantity;
    }

    public void setStockQuantity(BigDecimal stockQuantity) {
        this.stockQuantity = stockQuantity;
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

