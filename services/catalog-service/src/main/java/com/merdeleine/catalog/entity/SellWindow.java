package com.merdeleine.catalog.entity;

import com.merdeleine.catalog.enums.SellWindowStatus;
import jakarta.persistence.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "sell_window")
public class SellWindow {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(nullable = false, length = 50, unique = true)
    private String name;

    @Column(nullable = false)
    private OffsetDateTime startAt;

    @Column(nullable = false)
    private OffsetDateTime endAt;

    @Column(nullable = false, length = 50)
    private String timezone;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SellWindowStatus status = SellWindowStatus.DRAFT;

    @Column(name = "closed_at")
    private OffsetDateTime closedAt;

    // NEW: 付款時窗規則（confirm 後才開放付款）
    @Column(name = "payment_ttl_minutes", nullable = false)
    private int paymentTtlMinutes = 60 * 24; // default 24h（你可改）

    // NEW: 付款視窗起訖
    @Column(name = "payment_opened_at")
    private OffsetDateTime paymentOpenedAt;

    @Column(name = "payment_close_at")
    private OffsetDateTime paymentCloseAt;

    @Version
    @Column(nullable = false)
    private long version;

    @PrePersist
    void prePersist() {
        if (id == null) id = UUID.randomUUID();
        if (status == null) status = SellWindowStatus.DRAFT;
        // paymentTtlMinutes 已有預設值
    }

    // getters / setters (略，照你現有風格補齊)
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public OffsetDateTime getStartAt() { return startAt; }
    public void setStartAt(OffsetDateTime startAt) { this.startAt = startAt; }

    public OffsetDateTime getEndAt() { return endAt; }
    public void setEndAt(OffsetDateTime endAt) { this.endAt = endAt; }

    public String getTimezone() { return timezone; }
    public void setTimezone(String timezone) { this.timezone = timezone; }

    public SellWindowStatus getStatus() { return status; }
    public void setStatus(SellWindowStatus status) { this.status = status; }

    public OffsetDateTime getClosedAt() { return closedAt; }
    public void setClosedAt(OffsetDateTime closedAt) { this.closedAt = closedAt; }

    public int getPaymentTtlMinutes() { return paymentTtlMinutes; }
    public void setPaymentTtlMinutes(int paymentTtlMinutes) { this.paymentTtlMinutes = paymentTtlMinutes; }

    public OffsetDateTime getPaymentOpenedAt() { return paymentOpenedAt; }
    public void setPaymentOpenedAt(OffsetDateTime paymentOpenedAt) { this.paymentOpenedAt = paymentOpenedAt; }

    public OffsetDateTime getPaymentCloseAt() { return paymentCloseAt; }
    public void setPaymentCloseAt(OffsetDateTime paymentCloseAt) { this.paymentCloseAt = paymentCloseAt; }

    public long getVersion() { return version; }
}