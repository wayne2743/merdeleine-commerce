package com.merdeleine.catalog.service;

import com.merdeleine.catalog.dto.StockCreateRequest;
import com.merdeleine.catalog.dto.StockResponse;
import com.merdeleine.catalog.dto.StockUpdateRequest;
import com.merdeleine.catalog.entity.Ingredient;
import com.merdeleine.catalog.entity.Stock;
import com.merdeleine.catalog.exception.BadRequestException;
import com.merdeleine.catalog.exception.NotFoundException;
import com.merdeleine.catalog.repository.IngredientRepository;
import com.merdeleine.catalog.repository.StockRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class StockService {

    private final StockRepository stockRepository;
    private final IngredientRepository ingredientRepository;

    public StockService(StockRepository stockRepository, IngredientRepository ingredientRepository) {
        this.stockRepository = stockRepository;
        this.ingredientRepository = ingredientRepository;
    }

    public StockResponse create(StockCreateRequest request) {
        validateDateRange(request.getStockedAt(), request.getExpiresAt());

        Ingredient ingredient = ingredientRepository.findById(request.getIngredientId())
                .orElseThrow(() -> new NotFoundException("Ingredient not found: " + request.getIngredientId()));

        Stock stock = new Stock();
        stock.setIngredient(ingredient);
        stock.setUnitPriceCents(request.getUnitPriceCents());
        stock.setStockedAt(request.getStockedAt());
        stock.setExpiresAt(request.getExpiresAt());
        stock.setStockQuantity(request.getStockQuantity());

        return StockResponse.fromEntity(stockRepository.save(stock));
    }

    @Transactional(readOnly = true)
    public StockResponse getById(UUID id) {
        return StockResponse.fromEntity(findStock(id));
    }

    @Transactional(readOnly = true)
    public List<StockResponse> getByIngredientId(UUID ingredientId) {
        if (!ingredientRepository.existsById(ingredientId)) {
            throw new NotFoundException("Ingredient not found: " + ingredientId);
        }
        return stockRepository.findByIngredientId(ingredientId).stream()
                .map(StockResponse::fromEntity)
                .toList();
    }

    public StockResponse update(UUID id, StockUpdateRequest request) {
        Stock stock = findStock(id);

        LocalDate targetStockedAt = request.getStockedAt() != null ? request.getStockedAt() : stock.getStockedAt();
        LocalDate targetExpiresAt = request.getExpiresAt() != null ? request.getExpiresAt() : stock.getExpiresAt();
        validateDateRange(targetStockedAt, targetExpiresAt);

        if (request.getUnitPriceCents() != null) stock.setUnitPriceCents(request.getUnitPriceCents());
        if (request.getStockedAt() != null) stock.setStockedAt(request.getStockedAt());
        if (request.getExpiresAt() != null) stock.setExpiresAt(request.getExpiresAt());
        if (request.getStockQuantity() != null) stock.setStockQuantity(request.getStockQuantity());

        return StockResponse.fromEntity(stockRepository.save(stock));
    }

    public void delete(UUID id) {
        if (!stockRepository.existsById(id)) {
            throw new NotFoundException("Stock not found: " + id);
        }
        stockRepository.deleteById(id);
    }

    private Stock findStock(UUID id) {
        return stockRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Stock not found: " + id));
    }

    private void validateDateRange(LocalDate stockedAt, LocalDate expiresAt) {
        if (stockedAt != null && expiresAt != null && expiresAt.isBefore(stockedAt)) {
            throw new BadRequestException("expiresAt must be on or after stockedAt");
        }
    }
}
