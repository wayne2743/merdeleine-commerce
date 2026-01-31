package com.merdeleine.order.repository;


import com.merdeleine.order.entity.SellWindowQuota;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.UUID;

public interface SellWindowQuotaRepository extends JpaRepository<SellWindowQuota, UUID> {

    @Modifying
    @Query(value = """
        UPDATE sell_window_quota
        SET sold_qty = sold_qty + :qty,
            updated_at = NOW(),
            status = CASE WHEN sold_qty + :qty >= max_qty THEN 'CLOSED' ELSE status END
        WHERE sell_window_id = :sellWindowId
          AND product_id = :productId
          AND variant_id = :variantId
          AND status = 'OPEN'
          AND sold_qty + :qty <= max_qty
        """, nativeQuery = true)
    int tryReserve(UUID sellWindowId, UUID productId, UUID variantId, int qty);

    @Modifying
    @Query(value = """
        UPDATE sell_window_quota
        SET sold_qty = sold_qty - :qty,
            updated_at = NOW(),
            status = CASE WHEN sold_qty - :qty < max_qty THEN 'OPEN' ELSE status END
        WHERE sell_window_id = :sellWindowId
          AND product_id = :productId
          AND variant_id = :variantId
          AND sold_qty - :qty >= 0
        """, nativeQuery = true)
    int release(UUID sellWindowId, UUID productId, UUID variantId, int qty);
}
