package com.merdeleine.catalog.controller;

import com.merdeleine.catalog.dto.StockCreateRequest;
import com.merdeleine.catalog.dto.StockResponse;
import com.merdeleine.catalog.dto.StockUpdateRequest;
import com.merdeleine.catalog.service.StockService;
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
public class StockController {

    private final StockService stockService;

    public StockController(StockService stockService) {
        this.stockService = stockService;
    }

    @PostMapping("/stocks")
    public ResponseEntity<StockResponse> create(@Valid @RequestBody StockCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(stockService.create(request));
    }

    @GetMapping("/stocks/{id}")
    public ResponseEntity<StockResponse> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(stockService.getById(id));
    }

    @GetMapping("/ingredients/{ingredientId}/stocks")
    public ResponseEntity<List<StockResponse>> getByIngredientId(@PathVariable UUID ingredientId) {
        return ResponseEntity.ok(stockService.getByIngredientId(ingredientId));
    }

    @PutMapping("/stocks/{id}")
    public ResponseEntity<StockResponse> update(@PathVariable UUID id,
                                                @Valid @RequestBody StockUpdateRequest request) {
        return ResponseEntity.ok(stockService.update(id, request));
    }

    @DeleteMapping("/stocks/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        stockService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
