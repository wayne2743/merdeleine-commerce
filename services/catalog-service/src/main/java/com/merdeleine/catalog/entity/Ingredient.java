package com.merdeleine.catalog.entity;

import com.merdeleine.catalog.enums.IngredientAttribute;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "ingredient")
public class Ingredient {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 100)
    private String brand;

    @Column(length = 100)
    private String origin;

    @Column(name = "government_registration_info", length = 1000)
    private String governmentRegistrationInfo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private IngredientAttribute attribute;

    @Column(name = "calories_per_100g")
    private Integer caloriesPer100g;

    @Column(length = 500)
    private String allergens;

    @OneToMany(mappedBy = "ingredient", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Stock> stocks = new ArrayList<>();

    @Column(nullable = false)
    private OffsetDateTime createdAt;

    @Column(nullable = false)
    private OffsetDateTime updatedAt;

    @PrePersist
    void prePersist() {
        if (id == null) id = UUID.randomUUID();
        createdAt = OffsetDateTime.now();
        updatedAt = createdAt;
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = OffsetDateTime.now();
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getBrand() { return brand; }
    public void setBrand(String brand) { this.brand = brand; }

    public String getOrigin() { return origin; }
    public void setOrigin(String origin) { this.origin = origin; }

    public String getGovernmentRegistrationInfo() { return governmentRegistrationInfo; }
    public void setGovernmentRegistrationInfo(String governmentRegistrationInfo) {
        this.governmentRegistrationInfo = governmentRegistrationInfo;
    }

    public IngredientAttribute getAttribute() { return attribute; }
    public void setAttribute(IngredientAttribute attribute) { this.attribute = attribute; }

    public Integer getCaloriesPer100g() { return caloriesPer100g; }
    public void setCaloriesPer100g(Integer caloriesPer100g) { this.caloriesPer100g = caloriesPer100g; }

    public String getAllergens() { return allergens; }
    public void setAllergens(String allergens) { this.allergens = allergens; }

    public List<Stock> getStocks() { return stocks; }
    public void setStocks(List<Stock> stocks) { this.stocks = stocks; }

    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }

    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(OffsetDateTime updatedAt) { this.updatedAt = updatedAt; }
}
