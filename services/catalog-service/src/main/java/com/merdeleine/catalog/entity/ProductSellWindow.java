package com.merdeleine.catalog.entity;

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
    private SellWindow sellWindow;

    @Column(nullable = false)
    private int minTotalQty;

    @Column
    private Integer maxTotalQty;

    @Column
    private Integer leadDays;

    @Column
    private Integer shipDays;

    @Column(nullable = false)
    private boolean isClosed = true;

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

    public SellWindow getSellWindow() {
        return sellWindow;
    }

    public void setSellWindow(SellWindow sellWindow) {
        this.sellWindow = sellWindow;
    }

    public int getMinTotalQty() {
        return minTotalQty;
    }

    public void setMinTotalQty(int minTotalQty) {
        this.minTotalQty = minTotalQty;
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

    public boolean isClosed() {
        return isClosed;
    }

    public void setClosed(boolean closed) {
        isClosed = closed;
    }
}
