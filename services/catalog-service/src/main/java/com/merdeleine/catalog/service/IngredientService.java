package com.merdeleine.catalog.service;

import com.merdeleine.catalog.dto.IngredientCreateRequest;
import com.merdeleine.catalog.dto.IngredientResponse;
import com.merdeleine.catalog.dto.IngredientUpdateRequest;
import com.merdeleine.catalog.entity.Ingredient;
import com.merdeleine.catalog.exception.BadRequestException;
import com.merdeleine.catalog.exception.NotFoundException;
import com.merdeleine.catalog.repository.IngredientRepository;
import com.merdeleine.catalog.repository.ProductIngredientRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class IngredientService {

    private final IngredientRepository ingredientRepository;
    private final ProductIngredientRepository productIngredientRepository;

    public IngredientService(IngredientRepository ingredientRepository,
                             ProductIngredientRepository productIngredientRepository) {
        this.ingredientRepository = ingredientRepository;
        this.productIngredientRepository = productIngredientRepository;
    }

    public IngredientResponse create(IngredientCreateRequest request) {
        validateDateRange(request.getStockedAt(), request.getExpiresAt());

        Ingredient ingredient = new Ingredient();
        ingredient.setName(request.getName());
        ingredient.setUnitPriceCents(request.getUnitPriceCents());
        ingredient.setBrand(request.getBrand());
        ingredient.setOrigin(request.getOrigin());
        ingredient.setGovernmentRegistrationInfo(request.getGovernmentRegistrationInfo());
        ingredient.setAttribute(request.getAttribute());
        ingredient.setStockedAt(request.getStockedAt());
        ingredient.setExpiresAt(request.getExpiresAt());
        ingredient.setStockQuantity(request.getStockQuantity());

        return IngredientResponse.fromEntity(ingredientRepository.save(ingredient));
    }

    @Transactional(readOnly = true)
    public IngredientResponse getById(UUID id) {
        return IngredientResponse.fromEntity(findIngredient(id));
    }

    @Transactional(readOnly = true)
    public List<IngredientResponse> getAll() {
        return ingredientRepository.findAll().stream()
                .map(IngredientResponse::fromEntity)
                .toList();
    }

    public IngredientResponse update(UUID id, IngredientUpdateRequest request) {
        Ingredient ingredient = findIngredient(id);

        LocalDate targetStockedAt = request.getStockedAt() != null ? request.getStockedAt() : ingredient.getStockedAt();
        LocalDate targetExpiresAt = request.getExpiresAt() != null ? request.getExpiresAt() : ingredient.getExpiresAt();
        validateDateRange(targetStockedAt, targetExpiresAt);

        if (request.getName() != null) ingredient.setName(request.getName());
        if (request.getUnitPriceCents() != null) ingredient.setUnitPriceCents(request.getUnitPriceCents());
        if (request.getBrand() != null) ingredient.setBrand(request.getBrand());
        if (request.getOrigin() != null) ingredient.setOrigin(request.getOrigin());
        if (request.getGovernmentRegistrationInfo() != null) {
            ingredient.setGovernmentRegistrationInfo(request.getGovernmentRegistrationInfo());
        }
        if (request.getAttribute() != null) ingredient.setAttribute(request.getAttribute());
        if (request.getStockedAt() != null) ingredient.setStockedAt(request.getStockedAt());
        if (request.getExpiresAt() != null) ingredient.setExpiresAt(request.getExpiresAt());
        if (request.getStockQuantity() != null) ingredient.setStockQuantity(request.getStockQuantity());

        return IngredientResponse.fromEntity(ingredientRepository.save(ingredient));
    }

    public void delete(UUID id) {
        if (!ingredientRepository.existsById(id)) {
            throw new NotFoundException("Ingredient not found: " + id);
        }
        if (productIngredientRepository.existsByIngredientId(id)) {
            throw new BadRequestException("Ingredient is used by products and cannot be deleted: " + id);
        }
        ingredientRepository.deleteById(id);
    }

    private Ingredient findIngredient(UUID id) {
        return ingredientRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Ingredient not found: " + id));
    }

    private void validateDateRange(LocalDate stockedAt, LocalDate expiresAt) {
        if (stockedAt != null && expiresAt != null && expiresAt.isBefore(stockedAt)) {
            throw new BadRequestException("expiresAt must be on or after stockedAt");
        }
    }
}

