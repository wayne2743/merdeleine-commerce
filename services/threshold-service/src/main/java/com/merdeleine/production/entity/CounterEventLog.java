package com.merdeleine.production.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "counter_event_log")
public class CounterEventLog {

    @Id
    @Column(name = "id", columnDefinition = "UUID")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "counter_id", nullable = false, foreignKey = @ForeignKey(name = "fk_counter_event"))
    private BatchCounter counter;

    @Column(name = "source_event_type", nullable = false, length = 100)
    private String sourceEventType;

    @Column(name = "source_event_id", nullable = false, columnDefinition = "UUID")
    private UUID sourceEventId;

    @Column(name = "delta_qty", nullable = false)
    private Integer deltaQty;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    // Constructors
    public CounterEventLog() {
    }

    public CounterEventLog(UUID id, BatchCounter batchCounter, String sourceEventType, UUID sourceEventId, Integer deltaQty) {
        this.id = id;
        this.counter = batchCounter;
        this.sourceEventType = sourceEventType;
        this.sourceEventId = sourceEventId;
        this.deltaQty = deltaQty;
    }

    // Getters and Setters
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public BatchCounter getCounter() {
        return counter;
    }

    public void setCounter(BatchCounter counter) {
        this.counter = counter;
    }

    public String getSourceEventType() {
        return sourceEventType;
    }

    public void setSourceEventType(String sourceEventType) {
        this.sourceEventType = sourceEventType;
    }

    public UUID getSourceEventId() {
        return sourceEventId;
    }

    public void setSourceEventId(UUID sourceEventId) {
        this.sourceEventId = sourceEventId;
    }

    public Integer getDeltaQty() {
        return deltaQty;
    }

    public void setDeltaQty(Integer deltaQty) {
        this.deltaQty = deltaQty;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
