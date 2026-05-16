package com.merdeleine.catalog.repository;

import com.merdeleine.catalog.entity.Product;
import com.merdeleine.catalog.enums.ProductStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ProductRepository extends JpaRepository<Product, UUID> {
    List<Product> findByStatus(ProductStatus status);

    Page<Product> findByStatus(ProductStatus status, Pageable pageable);
}
