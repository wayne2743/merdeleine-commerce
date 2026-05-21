package com.merdeleine.catalog.service;

import com.merdeleine.catalog.dto.IngredientGroupCreateRequest;
import com.merdeleine.catalog.dto.IngredientGroupResponse;
import com.merdeleine.catalog.dto.IngredientGroupUpdateRequest;
import com.merdeleine.catalog.entity.IngredientGroup;
import com.merdeleine.catalog.entity.Product;
import com.merdeleine.catalog.exception.BadRequestException;
import com.merdeleine.catalog.exception.NotFoundException;
import com.merdeleine.catalog.repository.IngredientGroupRepository;
import com.merdeleine.catalog.repository.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class IngredientGroupService {

    private final IngredientGroupRepository ingredientGroupRepository;
    private final ProductRepository productRepository;

    public IngredientGroupService(IngredientGroupRepository ingredientGroupRepository,
                                  ProductRepository productRepository) {
        this.ingredientGroupRepository = ingredientGroupRepository;
        this.productRepository = productRepository;
    }

    public IngredientGroupResponse create(IngredientGroupCreateRequest request) {
        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new NotFoundException("Product not found: " + request.getProductId()));
        IngredientGroup group = new IngredientGroup();
        group.setProduct(product);
        group.setName(request.getName());
        return IngredientGroupResponse.fromEntity(ingredientGroupRepository.save(group));
    }

    @Transactional(readOnly = true)
    public IngredientGroupResponse getById(UUID id) {
        return IngredientGroupResponse.fromEntity(findOrThrow(id));
    }

    @Transactional(readOnly = true)
    public List<IngredientGroupResponse> getByProductId(UUID productId) {
        if (!productRepository.existsById(productId)) {
            throw new NotFoundException("Product not found: " + productId);
        }
        return ingredientGroupRepository.findByProductId(productId).stream()
                .map(IngredientGroupResponse::fromEntity)
                .toList();
    }

    public IngredientGroupResponse update(UUID id, IngredientGroupUpdateRequest request) {
        IngredientGroup group = findOrThrow(id);
        if (request.getName() != null) {
            if (request.getName().isBlank()) {
                throw new BadRequestException("name must not be blank");
            }
            group.setName(request.getName());
        }
        return IngredientGroupResponse.fromEntity(ingredientGroupRepository.save(group));
    }

    public void delete(UUID id) {
        if (!ingredientGroupRepository.existsById(id)) {
            throw new NotFoundException("IngredientGroup not found: " + id);
        }
        ingredientGroupRepository.deleteById(id);
    }

    private IngredientGroup findOrThrow(UUID id) {
        return ingredientGroupRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("IngredientGroup not found: " + id));
    }
}
