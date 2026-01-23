package com.merdeleine.catalog.dto;

import com.merdeleine.catalog.domain.ProductStatus;
import jakarta.validation.constraints.Size;

public class ProductUpdateRequest {
    
    @Size(max = 100, message = "Product name must not exceed 100 characters")
    private String name;
    
    @Size(max = 500, message = "Description must not exceed 500 characters")
    private String description;
    
    private ProductStatus status;

    public ProductUpdateRequest() {
    }

    public ProductUpdateRequest(String name, String description, ProductStatus status) {
        this.name = name;
        this.description = description;
        this.status = status;
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
}
