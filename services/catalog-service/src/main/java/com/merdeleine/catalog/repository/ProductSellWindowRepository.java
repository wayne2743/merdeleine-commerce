package com.merdeleine.catalog.repository;


import com.merdeleine.catalog.entity.Product;
import com.merdeleine.catalog.entity.ProductSellWindow;
import com.merdeleine.catalog.entity.SellWindow;
import com.merdeleine.catalog.enums.SellWindowStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
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

    @Query("""
        select
            psw.id as productSellWindowId,
    
            sw.id as sellWindowId,
            sw.name as sellWindowName,
            sw.startAt as startAt,
            sw.endAt as endAt,
            sw.timezone as timezone,
            sw.paymentCloseAt as paymentCloseAt,
    
            p.id as productId,
            p.name as productName,
    
            psw.unitPriceCents as unitPriceCents,
            psw.currency as currency,
    
            psw.minTotalQty as minQty,
            psw.maxTotalQty as maxQty
        from ProductSellWindow psw
        join psw.product p
        join psw.sellWindow sw
        order by sw.startAt desc, p.name asc
    """)
    Page<ProductSellWindowRow> pageRows(Pageable pageable);

    @Query("""
        select psw
        from ProductSellWindow psw
        join fetch psw.sellWindow sw
        join fetch psw.product p
        where p.id = :productId
          and psw.isClosed = false
          and sw.status = :status
          and sw.endAt > :now
        order by sw.endAt asc
    """)
    Optional<ProductSellWindow> findFirstActiveByProductId(UUID productId, SellWindowStatus status, OffsetDateTime now);


    @Query("""
        select
            psw.id as productSellWindowId,
            sw.id as sellWindowId,
            sw.name as sellWindowName,
            sw.startAt as startAt,
            sw.endAt as endAt,
            sw.timezone as timezone,
            sw.paymentCloseAt as paymentCloseAt,
            p.id as productId,
            p.name as productName,
            psw.unitPriceCents as unitPriceCents,
            psw.currency as currency,
            psw.minTotalQty as minQty,
            psw.maxTotalQty as maxQty
        from ProductSellWindow psw
        join psw.sellWindow sw
        join psw.product p
        where psw.id = :productSellWindowId
    """)
    Optional<ProductSellWindowRow> findRowByProductSellWindowId(@Param("productSellWindowId") UUID productSellWindowId);


    interface ProductSellWindowRow {
        UUID getProductSellWindowId();

        UUID getSellWindowId();
        String getSellWindowName();
        OffsetDateTime getStartAt();
        OffsetDateTime getEndAt();
        String getTimezone();
        OffsetDateTime getPaymentCloseAt();

        UUID getProductId();
        String getProductName();
        Integer getUnitPriceCents();
        String getCurrency();

        Integer getMinQty();
        Integer getMaxQty();
    }


}
