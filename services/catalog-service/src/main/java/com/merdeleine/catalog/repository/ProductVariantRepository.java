package com.merdeleine.catalog.repository;

import com.merdeleine.catalog.entity.Product;
import com.merdeleine.catalog.entity.ProductVariant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProductVariantRepository extends JpaRepository<ProductVariant, UUID> {
    List<ProductVariant> findByProduct(Product product);
    List<ProductVariant> findByProductId(UUID productId);
    Optional<ProductVariant> findBySku(String sku);
    List<ProductVariant> findByIsActive(boolean isActive);
}
