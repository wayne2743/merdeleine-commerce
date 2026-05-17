package com.merdeleine.catalog.service;


import com.merdeleine.catalog.dto.ProductCreateRequest;
import com.merdeleine.catalog.dto.ProductNextGroupOpenAtResponse;
import com.merdeleine.catalog.dto.PageResponse;
import com.merdeleine.catalog.dto.ProductIngredientRequest;
import com.merdeleine.catalog.dto.ProductResponse;
import com.merdeleine.catalog.dto.ProductUpdateRequest;
import com.merdeleine.catalog.entity.Ingredient;
import com.merdeleine.catalog.entity.Product;
import com.merdeleine.catalog.entity.ProductIngredient;
import com.merdeleine.catalog.enums.ProductStatus;
import com.merdeleine.catalog.exception.BadRequestException;
import com.merdeleine.catalog.exception.ProductNotFoundException;
import com.merdeleine.catalog.repository.IngredientRepository;
import com.merdeleine.catalog.repository.ProductImageRepository;
import com.merdeleine.catalog.repository.ProductIngredientRepository;
import com.merdeleine.catalog.repository.ProductRepository;
import com.merdeleine.catalog.repository.ProductSellWindowRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Transactional
public class ProductService {

    private static final int DEFAULT_MIN_QTY = 1;

    private final ProductRepository productRepository;
    private final ProductSellWindowRepository productSellWindowRepository;
    private final ProductImageRepository productImageRepository;
    private final IngredientRepository ingredientRepository;
    private final ProductIngredientRepository productIngredientRepository;

    public ProductService(ProductRepository productRepository,
                          ProductSellWindowRepository productSellWindowRepository,
                          ProductImageRepository productImageRepository,
                          IngredientRepository ingredientRepository,
                          ProductIngredientRepository productIngredientRepository) {
        this.productRepository = productRepository;
        this.productSellWindowRepository = productSellWindowRepository;
        this.productImageRepository = productImageRepository;
        this.ingredientRepository = ingredientRepository;
        this.productIngredientRepository = productIngredientRepository;
    }

    public ProductResponse createProduct(ProductCreateRequest request) {
        int defaultMinQty = request.getDefaultMinQty() != null ? request.getDefaultMinQty() : DEFAULT_MIN_QTY;
        Integer defaultMaxQty = request.getDefaultMaxQty();
        Integer defaultLeadDays = request.getDefaultLeadDays();
        Integer defaultShipDays = request.getDefaultShipDays();
        Integer defaultOpenDays = request.getDefaultOpenDays();
        validateDefaultQtyRange(defaultMinQty, defaultMaxQty);
        validateDefaultDayRange(defaultLeadDays, defaultShipDays, defaultOpenDays);

        Product product = new Product();
        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setStatus(request.getStatus() != null ? request.getStatus() : ProductStatus.DRAFT);
        product.setUnitPriceCents(request.getUnitPriceCents());
        product.setCurrency(request.getCurrency() != null ? request.getCurrency() : "TWD");
        product.setDefaultMinQty(defaultMinQty);
        product.setDefaultMaxQty(defaultMaxQty);
        product.setDefaultLeadDays(defaultLeadDays);
        product.setDefaultShipDays(defaultShipDays);
        product.setDefaultOpenDays(defaultOpenDays);
        applyProductIngredients(product, request.getProductIngredients());

        Product saved = productRepository.save(product);
        return ProductResponse.fromEntity(saved);
    }

    @Transactional(readOnly = true)
    public ProductResponse getProductById(UUID id) {
        Product product = productRepository.findByIdWithIngredients(id)
                .orElseThrow(() -> new ProductNotFoundException(id));
        return ProductResponse.fromEntity(product);
    }

    @Transactional(readOnly = true)
    public List<ProductResponse> getAllProducts() {
        return productRepository.findAllWithIngredients().stream()
                .map(ProductResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ProductResponse> getProductsByStatus(ProductStatus status) {
        return productRepository.findByStatusWithIngredients(status).stream()
                .map(ProductResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public PageResponse<ProductResponse> pageProducts(ProductStatus status, Pageable pageable) {
        int safePage = Math.max(pageable.getPageNumber(), 0);
        int safeSize = Math.min(Math.max(pageable.getPageSize(), 1), 100);
        PageRequest safePageRequest = PageRequest.of(safePage, safeSize, pageable.getSort());

        var page = status == null
                ? productRepository.findAll(safePageRequest)
                : productRepository.findByStatus(status, safePageRequest);

        List<UUID> productIds = page.getContent().stream()
                .map(Product::getId)
                .toList();

        Map<UUID, Product> productMap = productRepository.findByIdInWithIngredients(productIds).stream()
                .collect(Collectors.toMap(Product::getId, Function.identity(), (left, right) -> left, LinkedHashMap::new));

        List<ProductResponse> items = productIds.stream()
                .map(productMap::get)
                .filter(java.util.Objects::nonNull)
                .map(ProductResponse::fromEntity)
                .toList();

        return new PageResponse<>(items, safePage, safeSize, page.getTotalElements());
    }

    @Transactional(readOnly = true)
    public ProductNextGroupOpenAtResponse getProductNextGroupOpenAt(UUID productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ProductNotFoundException(productId));

        OffsetDateTime nextGroupOpenAt = productSellWindowRepository.findMaxSellWindowEndAtByProductId(productId);

        return new ProductNextGroupOpenAtResponse(
                nextGroupOpenAt,
                product.getDefaultMinQty(),
                product.getDefaultMaxQty(),
                product.getDefaultLeadDays(),
                product.getDefaultShipDays(),
                product.getDefaultOpenDays()
        );
    }

    public ProductResponse updateProduct(UUID id, ProductUpdateRequest request) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException(id));

        int targetMinQty = request.getDefaultMinQty() != null
                ? request.getDefaultMinQty()
                : product.getDefaultMinQty();
        Integer targetMaxQty = request.getDefaultMaxQty() != null
                ? request.getDefaultMaxQty()
                : product.getDefaultMaxQty();
        Integer targetLeadDays = request.getDefaultLeadDays() != null
                ? request.getDefaultLeadDays()
                : product.getDefaultLeadDays();
        Integer targetShipDays = request.getDefaultShipDays() != null
                ? request.getDefaultShipDays()
                : product.getDefaultShipDays();
        Integer targetOpenDays = request.getDefaultOpenDays() != null
                ? request.getDefaultOpenDays()
                : product.getDefaultOpenDays();
        validateDefaultQtyRange(targetMinQty, targetMaxQty);
        validateDefaultDayRange(targetLeadDays, targetShipDays, targetOpenDays);

        if (request.getName() != null) {
            product.setName(request.getName());
        }
        if (request.getDescription() != null) {
            product.setDescription(request.getDescription());
        }
        if (request.getStatus() != null) {
            product.setStatus(request.getStatus());
        }
        if (request.getUnitPriceCents() != null) {
            product.setUnitPriceCents(request.getUnitPriceCents());
        }
        if (request.getCurrency() != null) {
            product.setCurrency(request.getCurrency());
        }
        if (request.getDefaultMinQty() != null) {
            product.setDefaultMinQty(request.getDefaultMinQty());
        }
        if (request.getDefaultMaxQty() != null) {
            product.setDefaultMaxQty(request.getDefaultMaxQty());
        }
        if (request.getDefaultLeadDays() != null) {
            product.setDefaultLeadDays(request.getDefaultLeadDays());
        }
        if (request.getDefaultShipDays() != null) {
            product.setDefaultShipDays(request.getDefaultShipDays());
        }
        if (request.getDefaultOpenDays() != null) {
            product.setDefaultOpenDays(request.getDefaultOpenDays());
        }
        if (request.getProductIngredients() != null) {
            applyProductIngredients(product, request.getProductIngredients());
        }

        Product updated = productRepository.save(product);
        return ProductResponse.fromEntity(updated);
    }

    public void deleteProduct(UUID id) {
        if (!productRepository.existsById(id)) {
            throw new ProductNotFoundException(id);
        }

        // Delete dependent rows first to avoid FK violations on product delete.
        productImageRepository.deleteByProductId(id);
        productSellWindowRepository.deleteByProductId(id);
        productIngredientRepository.deleteByProductId(id);

        productRepository.deleteById(id);
    }

    private void validateDefaultQtyRange(int minQty, Integer maxQty) {
        if (minQty < 1) {
            throw new BadRequestException("defaultMinQty must be >= 1");
        }
        if (maxQty != null && maxQty < minQty) {
            throw new BadRequestException("defaultMaxQty must be >= defaultMinQty");
        }
    }

    private void validateDefaultDayRange(Integer leadDays, Integer shipDays, Integer openDays) {
        if (leadDays != null && leadDays < 0) {
            throw new BadRequestException("defaultLeadDays must be >= 0");
        }
        if (shipDays != null && shipDays < 0) {
            throw new BadRequestException("defaultShipDays must be >= 0");
        }
        if (openDays != null && openDays < 0) {
            throw new BadRequestException("defaultOpenDays must be >= 0");
        }
    }

    private void applyProductIngredients(Product product, List<ProductIngredientRequest> requests) {
        product.getProductIngredients().clear();
        if (requests == null || requests.isEmpty()) {
            return;
        }

        Set<UUID> uniqueIngredientIds = requests.stream()
                .map(ProductIngredientRequest::getIngredientId)
                .collect(Collectors.toSet());
        if (uniqueIngredientIds.size() != requests.size()) {
            throw new BadRequestException("Duplicate ingredientId found in productIngredients payload");
        }
        List<Ingredient> ingredients = ingredientRepository.findAllById(uniqueIngredientIds);
        Map<UUID, Ingredient> ingredientMap = ingredients.stream()
                .collect(Collectors.toMap(Ingredient::getId, Function.identity()));

        if (ingredientMap.size() != uniqueIngredientIds.size()) {
            Set<UUID> missing = uniqueIngredientIds.stream()
                    .filter(id -> !ingredientMap.containsKey(id))
                    .collect(Collectors.toSet());
            throw new BadRequestException("Ingredient not found: " + missing);
        }

        for (ProductIngredientRequest request : requests) {
            ProductIngredient pi = new ProductIngredient();
            pi.setProduct(product);
            pi.setIngredient(ingredientMap.get(request.getIngredientId()));
            pi.setRequiredAmount(request.getRequiredAmount());
            pi.setUnit(request.getUnit());
            product.getProductIngredients().add(pi);
        }
    }
}
