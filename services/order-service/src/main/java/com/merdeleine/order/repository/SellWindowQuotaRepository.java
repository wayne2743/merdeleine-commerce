package com.merdeleine.order.repository;


import com.merdeleine.order.entity.SellWindowQuota;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

public interface SellWindowQuotaRepository extends JpaRepository<SellWindowQuota, UUID>, SellWindowQuotaRepositoryCustom  {

    @Modifying
    @Query(value = """
        UPDATE sell_window_quota
        SET sold_qty = sold_qty + :qty,
            updated_at = NOW(),
            status = CASE WHEN sold_qty + :qty >= max_qty THEN 'CLOSED' ELSE status END
        WHERE sell_window_id = :sellWindowId
          AND product_id = :productId
          AND status = 'OPEN'
          AND sold_qty + :qty <= max_qty
        """, nativeQuery = true)
    int tryReserve(UUID sellWindowId, UUID productId, int qty);

    @Modifying
    @Query(value = """
        UPDATE sell_window_quota
        SET sold_qty = sold_qty - :qty,
            updated_at = NOW(),
            status = CASE WHEN sold_qty - :qty < max_qty THEN 'OPEN' ELSE status END
        WHERE sell_window_id = :sellWindowId
          AND product_id = :productId
          AND sold_qty - :qty >= 0
        """, nativeQuery = true)
    int release(UUID sellWindowId, UUID productId, int qty);


    @Modifying
    @Query(value = """
        INSERT INTO sell_window_quota (
            id,
            sell_window_id,
            product_id,
            min_qty,
            max_qty,
            sold_qty,
            status,
            updated_at
        )
        VALUES (
            :id,
            :sellWindowId,
            :productId,
            :minQty,
            :maxQty,
            0,
            'OPEN',
            NOW()
        )
        ON CONFLICT (sell_window_id, product_id)
        DO UPDATE SET
            min_qty = EXCLUDED.min_qty,
            max_qty = EXCLUDED.max_qty,
            updated_at = NOW()
        """, nativeQuery = true)
    void upsert(
            UUID id,
            UUID sellWindowId,
            UUID productId,
            int minQty,
            int maxQty
    );

    boolean existsBySellWindowIdAndProductId(UUID sellWindowId, UUID productId);


    @Modifying
    @Query("""
        update SellWindowQuota q
        set q.status = :closedStatus, q.updatedAt = :now
        where q.sellWindowId = :sellWindowId
          and q.productId = :productId
          and q.status <> :closedStatus
    """)
    int close(UUID sellWindowId, UUID productId, String closedStatus, OffsetDateTime now);


    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        select q
        from SellWindowQuota q
        where q.sellWindowId = :sellWindowId
          and q.productId = :productId
    """)
    Optional<SellWindowQuota> findForUpdate(
            @Param("sellWindowId") UUID sellWindowId,
            @Param("productId") UUID productId
    );

    Optional<SellWindowQuota> findBySellWindowIdAndProductId(UUID sellWindowId, UUID productId);
}
