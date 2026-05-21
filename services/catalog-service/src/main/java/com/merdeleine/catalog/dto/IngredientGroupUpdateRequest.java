package com.merdeleine.catalog.dto;

import jakarta.validation.constraints.Size;

public class IngredientGroupUpdateRequest {

    @Size(max = 100, message = "name must not exceed 100 characters")
    private String name;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
}
