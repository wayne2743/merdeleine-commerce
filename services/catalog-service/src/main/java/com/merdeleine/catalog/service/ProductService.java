package com.merdeleine.catalog.service;


import com.merdeleine.catalog.dto.ProductCreateRequest;
import com.merdeleine.catalog.dto.ProductNextGroupOpenAtResponse;
import com.merdeleine.catalog.dto.ProductResponse;
import com.merdeleine.catalog.dto.ProductUpdateRequest;
import com.merdeleine.catalog.entity.Product;
import com.merdeleine.catalog.enums.ProductStatus;
import com.merdeleine.catalog.exception.ProductNotFoundException;
import com.merdeleine.catalog.repository.ProductImageRepository;
import com.merdeleine.catalog.repository.ProductRepository;
import com.merdeleine.catalog.repository.ProductSellWindowRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.time.OffsetDateTime;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
public class ProductService {

    private static final int DEFAULT_MIN_QTY = 1;

    private final ProductRepository productRepository;
    private final ProductSellWindowRepository productSellWindowRepository;
    private final ProductImageRepository productImageRepository;

    public ProductService(ProductRepository productRepository,
                          ProductSellWindowRepository productSellWindowRepository,
                          ProductImageRepository productImageRepository) {
        this.productRepository = productRepository;
        this.productSellWindowRepository = productSellWindowRepository;
        this.productImageRepository = productImageRepository;
    }

    public ProductResponse createProduct(ProductCreateRequest request) {
        int defaultMinQty = request.getDefaultMinQty() != null ? request.getDefaultMinQty() : DEFAULT_MIN_QTY;
        Integer defaultMaxQty = request.getDefaultMaxQty();
        Integer defaultLeadDays = request.getDefaultLeadDays();
        Integer defaultShipDays = request.getDefaultShipDays();
        validateDefaultQtyRange(defaultMinQty, defaultMaxQty);
        validateDefaultDayRange(defaultLeadDays, defaultShipDays);

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

        Product saved = productRepository.save(product);
        return ProductResponse.fromEntity(saved);
    }

    @Transactional(readOnly = true)
    public ProductResponse getProductById(UUID id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException(id));
        return ProductResponse.fromEntity(product);
    }

    @Transactional(readOnly = true)
    public List<ProductResponse> getAllProducts() {
        return productRepository.findAll().stream()
                .map(ProductResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ProductResponse> getProductsByStatus(ProductStatus status) {
        return productRepository.findByStatus(status).stream()
                .map(ProductResponse::fromEntity)
                .collect(Collectors.toList());
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
                product.getDefaultShipDays()
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
        validateDefaultQtyRange(targetMinQty, targetMaxQty);
        validateDefaultDayRange(targetLeadDays, targetShipDays);
        
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

        productRepository.deleteById(id);
    }

    private void validateDefaultQtyRange(int minQty, Integer maxQty) {
        if (minQty < 1) {
            throw new IllegalArgumentException("defaultMinQty must be >= 1");
        }
        if (maxQty != null && maxQty < minQty) {
            throw new IllegalArgumentException("defaultMaxQty must be >= defaultMinQty");
        }
    }

    private void validateDefaultDayRange(Integer leadDays, Integer shipDays) {
        if (leadDays != null && leadDays < 0) {
            throw new IllegalArgumentException("defaultLeadDays must be >= 0");
        }
        if (shipDays != null && shipDays < 0) {
            throw new IllegalArgumentException("defaultShipDays must be >= 0");
        }
    }
}
