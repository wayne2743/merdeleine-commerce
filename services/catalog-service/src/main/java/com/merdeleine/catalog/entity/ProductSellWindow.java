package com.merdeleine.catalog.domain;

import com.merdeleine.catalog.entity.Product;
import jakarta.persistence.*;

import java.util.UUID;

@Entity
@Table(name = "product_sell_window")
public class ProductSellWindow {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sell_window_id", nullable = false)
    private com.merdeleine.catalog.domain.SellWindow sellWindow;

    @Column(nullable = false)
    private int thresholdQty;

    @Column
    private Integer maxTotalQty;

    @Column
    private Integer leadDays;

    @Column
    private Integer shipDays;

    @Column(nullable = false)
    private boolean enabled = true;

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

    public com.merdeleine.catalog.domain.SellWindow getSellWindow() {
        return sellWindow;
    }

    public void setSellWindow(com.merdeleine.catalog.domain.SellWindow sellWindow) {
        this.sellWindow = sellWindow;
    }

    public int getThresholdQty() {
        return thresholdQty;
    }

    public void setThresholdQty(int thresholdQty) {
        this.thresholdQty = thresholdQty;
    }

    public Integer getMaxTotalQty() {
        return maxTotalQty;
    }

    public void setMaxTotalQty(Integer maxTotalQty) {
        this.maxTotalQty = maxTotalQty;
    }

    public Integer getLeadDays() {
        return leadDays;
    }

    public void setLeadDays(Integer leadDays) {
        this.leadDays = leadDays;
    }

    public Integer getShipDays() {
        return shipDays;
    }

    public void setShipDays(Integer shipDays) {
        this.shipDays = shipDays;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}
