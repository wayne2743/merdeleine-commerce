package com.merdeleine.catalog.repository;

import com.merdeleine.catalog.entity.Stock;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface StockRepository extends JpaRepository<Stock, UUID> {

    List<Stock> findByIngredientId(UUID ingredientId);
}
