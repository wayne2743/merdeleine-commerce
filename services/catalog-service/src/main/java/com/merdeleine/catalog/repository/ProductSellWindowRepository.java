package com.merdeleine.catalog.repository;


import com.merdeleine.catalog.entity.Product;
import com.merdeleine.catalog.entity.ProductSellWindow;
import com.merdeleine.catalog.entity.SellWindow;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProductSellWindowRepository extends JpaRepository<ProductSellWindow, UUID> {
    List<ProductSellWindow> findByProduct(Product product);
    List<ProductSellWindow> findByProductId(UUID productId);
    List<ProductSellWindow> findBySellWindow(SellWindow sellWindow);
    List<ProductSellWindow> findBySellWindowId(UUID sellWindowId);
    Optional<ProductSellWindow> findByProductIdAndSellWindowId(UUID productId, UUID sellWindowId);
    List<ProductSellWindow> findByIsClosed(boolean isClosed);
    Optional<ProductSellWindow> findByProduct_IdAndSellWindow_Id(UUID productId, UUID sellWindowId);
    List<ProductSellWindow> findByProduct_Id(UUID productId);
    List<ProductSellWindow> findBySellWindow_Id(UUID sellWindowId);


    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
        update ProductSellWindow psw
        set psw.isClosed = true
        where psw.sellWindow.id = :sellWindowId
          and psw.isClosed = false
    """)
    int closeAllOpenBySellWindowId(@Param("sellWindowId") UUID sellWindowId);
}
