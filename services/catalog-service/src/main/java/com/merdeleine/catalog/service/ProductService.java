package com.merdeleine.catalog.service;


import com.merdeleine.catalog.dto.ProductCreateRequest;
import com.merdeleine.catalog.dto.ProductResponse;
import com.merdeleine.catalog.dto.ProductUpdateRequest;
import com.merdeleine.catalog.entity.Product;
import com.merdeleine.catalog.enums.ProductStatus;
import com.merdeleine.catalog.exception.ProductNotFoundException;
import com.merdeleine.catalog.repository.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
public class ProductService {

    private final ProductRepository productRepository;

    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    public ProductResponse createProduct(ProductCreateRequest request) {
        Product product = new Product();
        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setStatus(request.getStatus() != null ? request.getStatus() : ProductStatus.DRAFT);
        
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

    public ProductResponse updateProduct(UUID id, ProductUpdateRequest request) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException(id));
        
        if (request.getName() != null) {
            product.setName(request.getName());
        }
        if (request.getDescription() != null) {
            product.setDescription(request.getDescription());
        }
        if (request.getStatus() != null) {
            product.setStatus(request.getStatus());
        }
        
        Product updated = productRepository.save(product);
        return ProductResponse.fromEntity(updated);
    }

    public void deleteProduct(UUID id) {
        if (!productRepository.existsById(id)) {
            throw new ProductNotFoundException(id);
        }
        productRepository.deleteById(id);
    }
}
