package com.merdeleine.catalog.repository;

import com.merdeleine.catalog.domain.ProductSellWindow;
import com.merdeleine.catalog.entity.Product;
import com.merdeleine.catalog.domain.SellWindow;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProductSellWindowRepository extends JpaRepository<ProductSellWindow, UUID> {
    List<ProductSellWindow> findByProduct(Product product);
    List<ProductSellWindow> findByProductId(UUID productId);
    List<ProductSellWindow> findBySellWindow(SellWindow sellWindow);
    List<ProductSellWindow> findBySellWindowId(UUID sellWindowId);
    Optional<ProductSellWindow> findByProductIdAndSellWindowId(UUID productId, UUID sellWindowId);
    List<ProductSellWindow> findByEnabled(boolean enabled);
}
