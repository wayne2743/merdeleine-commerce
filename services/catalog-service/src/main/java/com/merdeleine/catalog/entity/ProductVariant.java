package com.merdeleine.catalog.entity;

import jakarta.persistence.*;

import java.util.UUID;

@Entity
@Table(name = "product_variant")
public class ProductVariant {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(nullable = false, length = 100, unique = true)
    private String sku;

    @Column(nullable = false, length = 100)
    private String variantName;

    @Column(nullable = false)
    private int priceCents;

    @Column(nullable = false, length = 10)
    private String currency;

    @Column(nullable = false)
    private boolean isActive = true;

    @PrePersist
    void prePersist() {
        if (id == null) id = UUID.randomUUID();
    }

    // getters / setters


    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public Product getProduct() {
        return product;
    }

    public void setProduct(Product product) {
        this.product = product;
    }

    public String getSku() {
        return sku;
    }

    public void setSku(String sku) {
        this.sku = sku;
    }

    public String getVariantName() {
        return variantName;
    }

    public void setVariantName(String variantName) {
        this.variantName = variantName;
    }

    public int getPriceCents() {
        return priceCents;
    }

    public void setPriceCents(int priceCents) {
        this.priceCents = priceCents;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
    }
}
