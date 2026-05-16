package com.merdeleine.catalog.repository;


import com.merdeleine.catalog.entity.Product;
import com.merdeleine.catalog.entity.ProductSellWindow;
import com.merdeleine.catalog.entity.SellWindow;
import com.merdeleine.catalog.enums.ProductStatus;
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

    @Query("""
        select psw
        from ProductSellWindow psw
        join fetch psw.product p
        join fetch psw.sellWindow sw
        where (:productId is null or p.id = :productId)
          and (:sellWindowId is null or sw.id = :sellWindowId)
        order by sw.startAt desc, p.name asc, psw.id asc
    """)
    List<ProductSellWindow> findAllWithRefs(
            @Param("productId") UUID productId,
            @Param("sellWindowId") UUID sellWindowId
    );

    @Query("""
        select max(sw.endAt)
        from ProductSellWindow psw
        join psw.sellWindow sw
        where psw.product.id = :productId
    """)
    OffsetDateTime findMaxSellWindowEndAtByProductId(@Param("productId") UUID productId);

    @Query("""
        select psw.id
        from ProductSellWindow psw
        join psw.sellWindow sw
        where psw.product.id = :productId
          and psw.isClosed = false
          and sw.status = com.merdeleine.catalog.enums.SellWindowStatus.OPEN
        order by sw.endAt asc
    """)
    List<UUID> findOpenProductSellWindowIdsByProductId(@Param("productId") UUID productId, Pageable pageable);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM ProductSellWindow psw WHERE psw.product.id = :productId")
    int deleteByProductId(@Param("productId") UUID productId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM ProductSellWindow psw WHERE psw.sellWindow.id = :sellWindowId")
    int deleteBySellWindowId(@Param("sellWindowId") UUID sellWindowId);


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
            psw.maxTotalQty as maxQty,
    
            psw.shipDays as shipDays,
            sw.predictedPaymentDate as predictedPaymentDate,
            sw.predictedProdDate as predictedProdDate,
            sw.predictedShipDate as predictedShipDate
        from ProductSellWindow psw
        join psw.product p
        join psw.sellWindow sw
        where psw.isClosed = false
          and sw.status = com.merdeleine.catalog.enums.SellWindowStatus.OPEN
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

    @Query(
            value = """
                select
                    psw.id as id,
                    psw.minTotalQty as minTotalQty,
                    psw.maxTotalQty as maxTotalQty,
                    psw.leadDays as leadDays,
                    psw.shipDays as shipDays,
                    psw.isClosed as isClosed,
                    p.id as productId,
                    p.name as productName,
                    p.description as productDescription,
                    p.status as productStatus,
                    p.unitPriceCents as productUnitPriceCents,
                    p.currency as productCurrency,
                    p.defaultMinQty as productDefaultMinQty,
                    p.defaultMaxQty as productDefaultMaxQty,
                    p.defaultLeadDays as productDefaultLeadDays,
                    p.defaultShipDays as productDefaultShipDays,
                    sw.id as sellWindowId,
                    sw.name as sellWindowName,
                    sw.status as sellWindowStatus,
                    sw.startAt as sellWindowStartAt,
                    sw.endAt as sellWindowEndAt,
                    sw.timezone as sellWindowTimezone,
                    sw.paymentTtlMinutes as sellWindowPaymentTtlMinutes,
                    sw.paymentOpenedAt as sellWindowPaymentOpenedAt,
                    sw.paymentCloseAt as sellWindowPaymentCloseAt,
                    sw.predictedPaymentDate as sellWindowPredictedPaymentDate,
                    sw.predictedProdDate as sellWindowPredictedProdDate,
                    sw.predictedShipDate as sellWindowPredictedShipDate
                from ProductSellWindow psw
                join psw.product p
                join psw.sellWindow sw
                where (:productId is null or p.id = :productId)
                  and (:sellWindowId is null or sw.id = :sellWindowId)
            """,
            countQuery = """
                select count(psw.id)
                from ProductSellWindow psw
                join psw.product p
                join psw.sellWindow sw
                where (:productId is null or p.id = :productId)
                  and (:sellWindowId is null or sw.id = :sellWindowId)
            """
    )
    Page<ProductSellWindowWithRefsRow> pageWithRefs(
            @Param("productId") UUID productId,
            @Param("sellWindowId") UUID sellWindowId,
            Pageable pageable
    );


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
            psw.maxTotalQty as maxQty,
            psw.shipDays as shipDays,
            sw.predictedPaymentDate as predictedPaymentDate,
            sw.predictedProdDate as predictedProdDate,
            sw.predictedShipDate as predictedShipDate
        from ProductSellWindow psw
        join psw.sellWindow sw
        join psw.product p
        where psw.id = :productSellWindowId
    """)
    Optional<ProductSellWindowRow> findRowByProductSellWindowId(@Param("productSellWindowId") UUID productSellWindowId);

    interface ProductSellWindowWithRefsRow {
        UUID getId();
        Integer getMinTotalQty();
        Integer getMaxTotalQty();
        Integer getLeadDays();
        Integer getShipDays();
        Boolean getIsClosed();

        UUID getProductId();
        String getProductName();
        String getProductDescription();
        ProductStatus getProductStatus();
        Integer getProductUnitPriceCents();
        String getProductCurrency();
        Integer getProductDefaultMinQty();
        Integer getProductDefaultMaxQty();
        Integer getProductDefaultLeadDays();
        Integer getProductDefaultShipDays();

        UUID getSellWindowId();
        String getSellWindowName();
        SellWindowStatus getSellWindowStatus();
        OffsetDateTime getSellWindowStartAt();
        OffsetDateTime getSellWindowEndAt();
        String getSellWindowTimezone();
        Integer getSellWindowPaymentTtlMinutes();
        OffsetDateTime getSellWindowPaymentOpenedAt();
        OffsetDateTime getSellWindowPaymentCloseAt();
        OffsetDateTime getSellWindowPredictedPaymentDate();
        OffsetDateTime getSellWindowPredictedProdDate();
        OffsetDateTime getSellWindowPredictedShipDate();
    }


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

        Integer getShipDays();
        OffsetDateTime getPredictedPaymentDate();
        OffsetDateTime getPredictedProdDate();
        OffsetDateTime getPredictedShipDate();
    }


}
