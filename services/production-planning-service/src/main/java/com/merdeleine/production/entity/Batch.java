package com.merdeleine.production.entity;

import com.merdeleine.production.enums.BatchStatus;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "batch")
public class Batch {

    @Id
    @Column(name = "id", columnDefinition = "UUID")
    private UUID id;

    @Column(name = "sell_window_id", nullable = false, columnDefinition = "UUID")
    private UUID sellWindowId;

    @Column(name = "product_id", nullable = false, columnDefinition = "UUID")
    private UUID productId;

    @Column(name = "target_qty", nullable = false)
    private Integer targetQty;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private BatchStatus status;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "confirmed_at")
    private OffsetDateTime confirmedAt;

    @OneToMany(mappedBy = "batch", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<BatchOrderLink> orderLinks = new ArrayList<>();

    @OneToOne(mappedBy = "batch", cascade = CascadeType.ALL, orphanRemoval = true)
    private BatchSchedule schedule;

    // Constructors
    public Batch() {
    }

    public Batch(UUID id, UUID sellWindowId, UUID productId, Integer targetQty, BatchStatus status) {
        this.id = id;
        this.sellWindowId = sellWindowId;
        this.productId = productId;
        this.targetQty = targetQty;
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

    public Integer getTargetQty() {
        return targetQty;
    }

    public void setTargetQty(Integer targetQty) {
        this.targetQty = targetQty;
    }

    public BatchStatus getStatus() {
        return status;
    }

    public void setStatus(BatchStatus status) {
        this.status = status;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public OffsetDateTime getConfirmedAt() {
        return confirmedAt;
    }

    public void setConfirmedAt(OffsetDateTime confirmedAt) {
        this.confirmedAt = confirmedAt;
    }

    public List<BatchOrderLink> getOrderLinks() {
        return orderLinks;
    }

    public void setOrderLinks(List<BatchOrderLink> orderLinks) {
        this.orderLinks = orderLinks;
    }

    public BatchSchedule getSchedule() {
        return schedule;
    }

    public void setSchedule(BatchSchedule schedule) {
        this.schedule = schedule;
        if (schedule != null) {
            schedule.setBatch(this);
        }
    }

    public void addOrderLink(BatchOrderLink orderLink) {
        orderLinks.add(orderLink);
        orderLink.setBatch(this);
    }

    public void removeOrderLink(BatchOrderLink orderLink) {
        orderLinks.remove(orderLink);
        orderLink.setBatch(null);
    }
}
