package com.merdeleine.order.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "sell_window_quota",
        uniqueConstraints = @UniqueConstraint(name = "uq_quota",
                columnNames = {"sell_window_id", "product_id", "variant_id"})
)
public class SellWindowQuota {

    @Id
    @Column(name = "id", columnDefinition = "UUID")
    private UUID id;

    @Column(name = "sell_window_id", nullable = false, columnDefinition = "UUID")
    private UUID sellWindowId;

    @Column(name = "product_id", nullable = false, columnDefinition = "UUID")
    private UUID productId;

    @Column(name = "variant_id", nullable = false, columnDefinition = "UUID")
    private UUID variantId;

    @Column(name = "min_qty", nullable = false)
    private Integer minQty;

    @Column(name = "max_qty", nullable = false)
    private Integer maxQty;

    @Column(name = "sold_qty", nullable = false)
    private Integer soldQty;

    @Column(name = "status", nullable = false, length = 20)
    private String status; // OPEN / CLOSED

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    // getters/setters 省略（或用 Lombok）
}
