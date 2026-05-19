package com.merdeleine.catalog.dto;

import com.merdeleine.catalog.enums.IngredientAttribute;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

public class IngredientUpdateRequest {

    @Size(max = 100, message = "name must not exceed 100 characters")
    private String name;

    @Size(max = 100, message = "brand must not exceed 100 characters")
    private String brand;

    @Size(max = 100, message = "origin must not exceed 100 characters")
    private String origin;

    @Size(max = 1000, message = "governmentRegistrationInfo must not exceed 1000 characters")
    private String governmentRegistrationInfo;

    private IngredientAttribute attribute;

    @Min(value = 0, message = "caloriesPer100g must be >= 0")
    private Integer caloriesPer100g;

    @Size(max = 500, message = "allergens must not exceed 500 characters")
    private String allergens;

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
}
