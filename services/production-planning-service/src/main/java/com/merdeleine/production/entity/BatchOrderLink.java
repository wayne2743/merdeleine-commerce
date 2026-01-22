package com.merdeleine.production.entity;

import jakarta.persistence.*;

import java.util.UUID;

@Entity
@Table(name = "batch_order_link")
public class BatchOrderLink {

    @Id
    @Column(name = "id", columnDefinition = "UUID")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "batch_id", nullable = false, foreignKey = @ForeignKey(name = "fk_batch_order"))
    private Batch batch;

    @Column(name = "order_id", nullable = false, columnDefinition = "UUID")
    private UUID orderId;

    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    // Constructors
    public BatchOrderLink() {
    }

    public BatchOrderLink(UUID id, UUID orderId, Integer quantity) {
        this.id = id;
        this.orderId = orderId;
        this.quantity = quantity;
    }

    // Getters and Setters
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public Batch getBatch() {
        return batch;
    }

    public void setBatch(Batch batch) {
        this.batch = batch;
    }

    public UUID getOrderId() {
        return orderId;
    }

    public void setOrderId(UUID orderId) {
        this.orderId = orderId;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }
}
