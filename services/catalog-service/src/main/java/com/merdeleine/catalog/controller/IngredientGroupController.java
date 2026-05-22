package com.merdeleine.catalog.controller;

import com.merdeleine.catalog.dto.IngredientGroupCreateRequest;
import com.merdeleine.catalog.dto.IngredientGroupResponse;
import com.merdeleine.catalog.dto.IngredientGroupUpdateRequest;
import com.merdeleine.catalog.service.IngredientGroupService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
public class IngredientGroupController {

    private final IngredientGroupService ingredientGroupService;

    public IngredientGroupController(IngredientGroupService ingredientGroupService) {
        this.ingredientGroupService = ingredientGroupService;
    }

    @PostMapping("/ingredient-groups")
    public ResponseEntity<IngredientGroupResponse> create(@Valid @RequestBody IngredientGroupCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ingredientGroupService.create(request));
    }

    @GetMapping("/ingredient-groups")
    public ResponseEntity<List<IngredientGroupResponse>> getAll() {
        return ResponseEntity.ok(ingredientGroupService.getAll());
    }

    @GetMapping("/ingredient-groups/{id}")
    public ResponseEntity<IngredientGroupResponse> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(ingredientGroupService.getById(id));
    }

    @GetMapping("/products/{productId}/ingredient-groups")
    public ResponseEntity<List<IngredientGroupResponse>> getByProductId(@PathVariable UUID productId) {
        return ResponseEntity.ok(ingredientGroupService.getByProductId(productId));
    }

    @PutMapping("/ingredient-groups/{id}")
    public ResponseEntity<IngredientGroupResponse> update(@PathVariable UUID id,
                                                          @Valid @RequestBody IngredientGroupUpdateRequest request) {
        return ResponseEntity.ok(ingredientGroupService.update(id, request));
    }

    @DeleteMapping("/ingredient-groups/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        ingredientGroupService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
