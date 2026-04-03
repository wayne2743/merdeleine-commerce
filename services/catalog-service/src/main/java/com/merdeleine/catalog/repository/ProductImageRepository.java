package com.merdeleine.catalog.repository;

import com.merdeleine.catalog.entity.ProductImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProductImageRepository extends JpaRepository<ProductImage, UUID> {

    List<ProductImage> findByProductIdOrderBySortOrderAsc(UUID productId);

    List<ProductImage> findByProductIdAndIsActiveOrderBySortOrderAsc(UUID productId, Boolean isActive);

    Optional<ProductImage> findByIdAndProductId(UUID id, UUID productId);

    boolean existsByProductIdAndIsPrimaryTrue(UUID productId);

    @Modifying
    @Query("UPDATE ProductImage pi SET pi.isPrimary = false WHERE pi.product.id = :productId AND pi.id <> :excludeId")
    void clearPrimaryByProductId(@Param("productId") UUID productId, @Param("excludeId") UUID excludeId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM ProductImage pi WHERE pi.product.id = :productId")
    int deleteByProductId(@Param("productId") UUID productId);
}

