package com.merdeleine.catalog.controller;

import com.merdeleine.catalog.dto.IngredientCreateRequest;
import com.merdeleine.catalog.dto.IngredientResponse;
import com.merdeleine.catalog.dto.IngredientUpdateRequest;
import com.merdeleine.catalog.service.IngredientService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/ingredients")
public class IngredientController {

    private final IngredientService ingredientService;

    public IngredientController(IngredientService ingredientService) {
        this.ingredientService = ingredientService;
    }

    @PostMapping
    public ResponseEntity<IngredientResponse> create(@Valid @RequestBody IngredientCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ingredientService.create(request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<IngredientResponse> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(ingredientService.getById(id));
    }

    @GetMapping
    public ResponseEntity<List<IngredientResponse>> getAll() {
        return ResponseEntity.ok(ingredientService.getAll());
    }

    @PutMapping("/{id}")
    public ResponseEntity<IngredientResponse> update(@PathVariable UUID id,
                                                     @Valid @RequestBody IngredientUpdateRequest request) {
        return ResponseEntity.ok(ingredientService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        ingredientService.delete(id);
        return ResponseEntity.noContent().build();
    }
}

