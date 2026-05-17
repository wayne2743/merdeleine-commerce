package com.merdeleine.catalog.repository;

import com.merdeleine.catalog.entity.ProductIngredient;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface ProductIngredientRepository extends JpaRepository<ProductIngredient, UUID> {

    boolean existsByIngredientId(UUID ingredientId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM ProductIngredient pi WHERE pi.product.id = :productId")
    int deleteByProductId(@Param("productId") UUID productId);
}

