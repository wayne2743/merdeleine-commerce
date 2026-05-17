package com.merdeleine.catalog.repository;

import com.merdeleine.catalog.entity.Product;
import com.merdeleine.catalog.enums.ProductStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProductRepository extends JpaRepository<Product, UUID> {
    @Query("""
            select distinct p from Product p
            left join fetch p.productIngredients pi
            left join fetch pi.ingredient i
            where p.id = :id
            """)
    Optional<Product> findByIdWithIngredients(@Param("id") UUID id);

    @Query("""
            select distinct p from Product p
            left join fetch p.productIngredients pi
            left join fetch pi.ingredient i
            """)
    List<Product> findAllWithIngredients();

    @Query("""
            select distinct p from Product p
            left join fetch p.productIngredients pi
            left join fetch pi.ingredient i
            where p.status = :status
            """)
    List<Product> findByStatusWithIngredients(@Param("status") ProductStatus status);

    @Query("""
            select distinct p from Product p
            left join fetch p.productIngredients pi
            left join fetch pi.ingredient i
            where p.id in :ids
            """)
    List<Product> findByIdInWithIngredients(@Param("ids") List<UUID> ids);

    List<Product> findByStatus(ProductStatus status);

    Page<Product> findByStatus(ProductStatus status, Pageable pageable);
}
