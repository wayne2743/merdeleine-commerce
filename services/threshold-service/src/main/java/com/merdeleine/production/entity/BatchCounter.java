package com.merdeleine.production.entity;

import com.merdeleine.production.enums.BatchCounterStatus;
import jakarta.persistence.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "batch_counter", 
       uniqueConstraints = @UniqueConstraint(name = "uq_counter", columnNames = {"sell_window_id", "product_id"}))
public class BatchCounter {

    @Id
    @Column(name = "id", columnDefinition = "UUID")
    private UUID id;

    @Column(name = "sell_window_id", nullable = false, columnDefinition = "UUID")
    private UUID sellWindowId;

    @Column(name = "product_id", nullable = false, columnDefinition = "UUID")
    private UUID productId;

    @Column(name = "paid_qty", nullable = false)
    private Integer paidQty = 0;

    @Column(name = "threshold_qty", nullable = false)
    private Integer thresholdQty;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private BatchCounterStatus status;

    @Column(name = "reached_at")
    private OffsetDateTime reachedAt;

    @Column(name = "reached_event_id", columnDefinition = "UUID")
    private UUID reachedEventId;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @OneToMany(mappedBy = "counter", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CounterEventLog> eventLogs = new ArrayList<>();

    // Constructors
    public BatchCounter() {
    }

    public BatchCounter(UUID id, UUID sellWindowId, UUID productId, Integer thresholdQty, BatchCounterStatus status) {
        this.id = id;
        this.sellWindowId = sellWindowId;
        this.productId = productId;
        this.thresholdQty = thresholdQty;
        this.status = status;
    }

    // Getters and Setters
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getSellWindowId() {
        return sellWindowId;
    }

    public void setSellWindowId(UUID sellWindowId) {
        this.sellWindowId = sellWindowId;
    }

    public UUID getProductId() {
        return productId;
    }

    public void setProductId(UUID productId) {
        this.productId = productId;
    }

    public Integer getPaidQty() {
        return paidQty;
    }

    public void setPaidQty(Integer paidQty) {
        this.paidQty = paidQty;
    }

    public Integer getThresholdQty() {
        return thresholdQty;
    }

    public void setThresholdQty(Integer thresholdQty) {
        this.thresholdQty = thresholdQty;
    }

    public BatchCounterStatus getStatus() {
        return status;
    }

    public void setStatus(BatchCounterStatus status) {
        this.status = status;
    }

    public OffsetDateTime getReachedAt() {
        return reachedAt;
    }

    public void setReachedAt(OffsetDateTime reachedAt) {
        this.reachedAt = reachedAt;
    }

    public UUID getReachedEventId() {
        return reachedEventId;
    }

    public void setReachedEventId(UUID reachedEventId) {
        this.reachedEventId = reachedEventId;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(OffsetDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public List<CounterEventLog> getEventLogs() {
        return eventLogs;
    }

    public void setEventLogs(List<CounterEventLog> eventLogs) {
        this.eventLogs = eventLogs;
    }

    public void addEventLog(CounterEventLog eventLog) {
        eventLogs.add(eventLog);
        eventLog.setCounter(this);
    }

    public void removeEventLog(CounterEventLog eventLog) {
        eventLogs.remove(eventLog);
        eventLog.setCounter(null);
    }
}
