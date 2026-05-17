package com.merdeleine.catalog.dto;


import com.merdeleine.catalog.enums.ProductStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

import java.util.List;

public class ProductUpdateRequest {
    
    @Size(max = 100, message = "Product name must not exceed 100 characters")
    private String name;

    @Size(max = 500, message = "Description must not exceed 500 characters")
    private String description;
    
    private ProductStatus status;

    @Min(value = 0, message = "Unit price must be >= 0")
    private Integer unitPriceCents;

    @Size(max = 10, message = "Currency must not exceed 10 characters")
    private String currency;

    @Min(value = 1, message = "defaultMinQty must be >= 1")
    private Integer defaultMinQty;

    @Min(value = 1, message = "defaultMaxQty must be >= 1")
    private Integer defaultMaxQty;

    @Min(value = 0, message = "defaultLeadDays must be >= 0")
    private Integer defaultLeadDays;

    @Min(value = 0, message = "defaultShipDays must be >= 0")
    private Integer defaultShipDays;

    @Min(value = 0, message = "defaultOpenDays must be >= 0")
    private Integer defaultOpenDays;

    /** 商品成分 */
    private String ingredients;

    /** 商品過敏原 */
    private String allergens;

    /** 每份卡路里 (kcal) */
    @Min(value = 0, message = "calories must be >= 0")
    private Integer calories;

    /** 商品所需原料清單；有傳則全量覆蓋 */
    @Valid
    private List<ProductIngredientRequest> productIngredients;

    public ProductUpdateRequest() {
    }

    public ProductUpdateRequest(String name, String description, ProductStatus status, Integer unitPriceCents, String currency,
                                Integer defaultMinQty, Integer defaultMaxQty,
                                Integer defaultLeadDays, Integer defaultShipDays,
                                Integer defaultOpenDays) {
        this.name = name;
        this.description = description;
        this.status = status;
        this.unitPriceCents = unitPriceCents;
        this.currency = currency;
        this.defaultMinQty = defaultMinQty;
        this.defaultMaxQty = defaultMaxQty;
        this.defaultLeadDays = defaultLeadDays;
        this.defaultShipDays = defaultShipDays;
        this.defaultOpenDays = defaultOpenDays;
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

    public Integer getDefaultOpenDays() {
        return defaultOpenDays;
    }

    public void setDefaultOpenDays(Integer defaultOpenDays) {
        this.defaultOpenDays = defaultOpenDays;
    }

    public String getIngredients() {
        return ingredients;
    }

    public void setIngredients(String ingredients) {
        this.ingredients = ingredients;
    }

    public String getAllergens() {
        return allergens;
    }

    public void setAllergens(String allergens) {
        this.allergens = allergens;
    }

    public Integer getCalories() {
        return calories;
    }

    public void setCalories(Integer calories) {
        this.calories = calories;
    }

    public List<ProductIngredientRequest> getProductIngredients() {
        return productIngredients;
    }

    public void setProductIngredients(List<ProductIngredientRequest> productIngredients) {
        this.productIngredients = productIngredients;
    }
}
