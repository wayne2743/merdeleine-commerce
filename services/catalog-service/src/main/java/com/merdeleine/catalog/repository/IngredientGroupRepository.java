package com.merdeleine.catalog.repository;

import com.merdeleine.catalog.entity.IngredientGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface IngredientGroupRepository extends JpaRepository<IngredientGroup, UUID> {
    @Query("""
            SELECT DISTINCT pi.ingredientGroup
            FROM ProductIngredient pi
            WHERE pi.product.id = :productId AND pi.ingredientGroup IS NOT NULL
            """)
    List<IngredientGroup> findByProductId(@Param("productId") UUID productId);
}
