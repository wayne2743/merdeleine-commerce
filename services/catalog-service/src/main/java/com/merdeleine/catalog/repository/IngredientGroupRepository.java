package com.merdeleine.catalog.repository;

import com.merdeleine.catalog.entity.IngredientGroup;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface IngredientGroupRepository extends JpaRepository<IngredientGroup, UUID> {
    List<IngredientGroup> findByProductId(UUID productId);
    void deleteByProductId(UUID productId);
}
