package com.merdeleine.order.entity;

import jakarta.persistence.*;

import java.util.UUID;

@Entity
@Table(name = "order_item")
public class OrderItem {

    @Id
    @Column(name = "id", columnDefinition = "UUID")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false, foreignKey = @ForeignKey(name = "fk_order_item_order"))
    private Order order;

    @Column(name = "product_id", nullable = false, columnDefinition = "UUID")
    private UUID productId;

    @Column(name = "variant_id", nullable = false, columnDefinition = "UUID")
    private UUID variantId;

    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    @Column(name = "unit_price_cents", nullable = false)
    private Integer unitPriceCents;

    @Column(name = "subtotal_cents", nullable = false)
    private Integer subtotalCents;

    // Constructors
    public OrderItem() {
    }

    public OrderItem(UUID id, UUID productId, UUID variantId, Integer quantity, 
                     Integer unitPriceCents, Integer subtotalCents) {
        this.id = id;
        this.productId = productId;
        this.variantId = variantId;
        this.quantity = quantity;
        this.unitPriceCents = unitPriceCents;
        this.subtotalCents = subtotalCents;
    }

    // Getters and Setters
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public Order getOrder() {
        return order;
    }

    public void setOrder(Order order) {
        this.order = order;
    }

    public UUID getProductId() {
        return productId;
    }

    public void setProductId(UUID productId) {
        this.productId = productId;
    }

    public UUID getVariantId() {
        return variantId;
    }

    public void setVariantId(UUID variantId) {
        this.variantId = variantId;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public Integer getUnitPriceCents() {
        return unitPriceCents;
    }

    public void setUnitPriceCents(Integer unitPriceCents) {
        this.unitPriceCents = unitPriceCents;
    }

    public Integer getSubtotalCents() {
        return subtotalCents;
    }

    public void setSubtotalCents(Integer subtotalCents) {
        this.subtotalCents = subtotalCents;
    }
}
