package com.merdeleine.production.entity;

import com.merdeleine.production.enums.CounterStatus;
import jakarta.persistence.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Entity
@Table(
        name = "batch_counter",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_counter", columnNames = {"sell_window_id", "product_id"})
        },
        indexes = {
                @Index(name = "idx_counter_lookup", columnList = "sell_window_id, product_id"),
                @Index(name = "idx_counter_status_updated_at", columnList = "status, updated_at")
        }
)
public class BatchCounter {

    @Id
    @Column(name = "id", columnDefinition = "UUID")
    private UUID id;

    @Column(name = "sell_window_id", nullable = false, columnDefinition = "UUID")
    private UUID sellWindowId;

    @Column(name = "product_id", nullable = false, columnDefinition = "UUID")
    private UUID productId;

    @Column(name = "reserved_qty", nullable = false)
    private Integer reservedQty = 0;

    @Column(name = "paid_qty", nullable = false)
    private Integer paidQty = 0;

    @Column(name = "threshold_qty", nullable = false)
    private Integer thresholdQty;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private CounterStatus status = CounterStatus.OPEN;

    @Column(name = "reached_at")
    private OffsetDateTime reachedAt;

    @Column(name = "reached_event_id", columnDefinition = "UUID")
    private UUID reachedEventId;

    @Column(name = "final_paid_qty")
    private Integer finalPaidQty;

    @Column(name = "finalized_at")
    private OffsetDateTime finalizedAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @OneToMany(mappedBy = "counter", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CounterEventLog> counterEventLogs;

    @PrePersist
    void prePersist() {
        if (id == null) id = UUID.randomUUID();
        if (reservedQty == null) reservedQty = 0;
        if (paidQty == null) paidQty = 0;
        if (status == null) status = CounterStatus.OPEN;
    }

    // getters/setters

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getSellWindowId() { return sellWindowId; }
    public void setSellWindowId(UUID sellWindowId) { this.sellWindowId = sellWindowId; }

    public UUID getProductId() { return productId; }
    public void setProductId(UUID productId) { this.productId = productId; }

    public Integer getReservedQty() { return reservedQty; }
    public void setReservedQty(Integer reservedQty) { this.reservedQty = reservedQty; }

    public Integer getPaidQty() { return paidQty; }
    public void setPaidQty(Integer paidQty) { this.paidQty = paidQty; }

    public Integer getThresholdQty() { return thresholdQty; }
    public void setThresholdQty(Integer thresholdQty) { this.thresholdQty = thresholdQty; }

    public CounterStatus getStatus() { return status; }
    public void setStatus(CounterStatus status) { this.status = status; }

    public OffsetDateTime getReachedAt() { return reachedAt; }
    public void setReachedAt(OffsetDateTime reachedAt) { this.reachedAt = reachedAt; }

    public UUID getReachedEventId() { return reachedEventId; }
    public void setReachedEventId(UUID reachedEventId) { this.reachedEventId = reachedEventId; }

    public Integer getFinalPaidQty() { return finalPaidQty; }
    public void setFinalPaidQty(Integer finalPaidQty) { this.finalPaidQty = finalPaidQty; }

    public OffsetDateTime getFinalizedAt() { return finalizedAt; }
    public void setFinalizedAt(OffsetDateTime finalizedAt) { this.finalizedAt = finalizedAt; }

    public OffsetDateTime getUpdatedAt() { return updatedAt; }

    public List<CounterEventLog> getCounterEventLogs() {
        return counterEventLogs;
    }

    public void setCounterEventLogs(List<CounterEventLog> counterEventLogs) {
        this.counterEventLogs = counterEventLogs;
    }

    public void addEventLog(CounterEventLog log) {
        counterEventLogs.add(log);
        log.setCounter(this);
    }
}
