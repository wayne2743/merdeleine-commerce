package com.merdeleine.order.entity;

import com.merdeleine.order.enums.OrderStatus;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "orders",
        indexes = {
                @Index(name = "idx_orders_status_due_at", columnList = "status, payment_due_at"),
                @Index(name = "idx_orders_sell_window_status", columnList = "sell_window_id, status")
        }
)
public class Order {

    @Id
    @Column(name = "id", columnDefinition = "UUID")
    private UUID id;

    @Column(name = "order_no", nullable = false, length = 50, unique = true)
    private String orderNo;

    @Column(name = "customer_id", nullable = false, columnDefinition = "UUID")
    private UUID customerId;

    @Column(name = "sell_window_id", columnDefinition = "UUID")
    private UUID sellWindowId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private OrderStatus status;

    @Column(name = "total_amount_cents", nullable = false)
    private Integer totalAmountCents;

    @Column(name = "currency", nullable = false, length = 10)
    private String currency;

    @Column(name = "contact_name", length = 100)
    private String contactName;

    @Column(name = "contact_phone", length = 30)
    private String contactPhone;

    @Column(name = "contact_email", length = 255)
    private String contactEmail;

    @Column(name = "shipping_address", columnDefinition = "TEXT")
    private String shippingAddress;

    @Column(name = "payment_due_at")
    private OffsetDateTime paymentDueAt;

    @Column(name = "payment_failed_count", nullable = false)
    private Integer paymentFailedCount = 0;

    @Column(name = "last_payment_error", columnDefinition = "TEXT")
    private String lastPaymentError;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @OneToOne(
            mappedBy = "order",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.LAZY
    )
    private OrderItem item;

    // Constructors
    public Order() {
    }

    public Order(UUID id, String orderNo, UUID customerId, OrderStatus status,
                 Integer totalAmountCents, String currency) {
        this.id = id;
        this.orderNo = orderNo;
        this.customerId = customerId;
        this.status = status;
        this.totalAmountCents = totalAmountCents;
        this.currency = currency;
    }

    @PrePersist
    void prePersist() {
        if (id == null) id = UUID.randomUUID();
        if (paymentFailedCount == null) paymentFailedCount = 0;
    }

    /* ---------- domain methods ---------- */

    public void setItem(OrderItem item) {
        if (this.item != null) {
            this.item.setOrder(null);
        }
        this.item = item;
        if (item != null) {
            item.setOrder(this);
        }
    }

    public void clearItem() {
        if (this.item != null) {
            this.item.setOrder(null);
            this.item = null;
        }
    }

    // getters/setters

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getOrderNo() { return orderNo; }
    public void setOrderNo(String orderNo) { this.orderNo = orderNo; }

    public UUID getCustomerId() { return customerId; }
    public void setCustomerId(UUID customerId) { this.customerId = customerId; }

    public UUID getSellWindowId() { return sellWindowId; }
    public void setSellWindowId(UUID sellWindowId) { this.sellWindowId = sellWindowId; }

    public OrderStatus getStatus() { return status; }
    public void setStatus(OrderStatus status) { this.status = status; }

    public Integer getTotalAmountCents() { return totalAmountCents; }
    public void setTotalAmountCents(Integer totalAmountCents) { this.totalAmountCents = totalAmountCents; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public String getContactName() { return contactName; }
    public void setContactName(String contactName) { this.contactName = contactName; }

    public String getContactPhone() { return contactPhone; }
    public void setContactPhone(String contactPhone) { this.contactPhone = contactPhone; }

    public String getContactEmail() { return contactEmail; }
    public void setContactEmail(String contactEmail) { this.contactEmail = contactEmail; }

    public String getShippingAddress() { return shippingAddress; }
    public void setShippingAddress(String shippingAddress) { this.shippingAddress = shippingAddress; }

    public OffsetDateTime getPaymentDueAt() { return paymentDueAt; }
    public void setPaymentDueAt(OffsetDateTime paymentDueAt) { this.paymentDueAt = paymentDueAt; }

    public Integer getPaymentFailedCount() { return paymentFailedCount; }
    public void setPaymentFailedCount(Integer paymentFailedCount) { this.paymentFailedCount = paymentFailedCount; }

    public String getLastPaymentError() { return lastPaymentError; }
    public void setLastPaymentError(String lastPaymentError) { this.lastPaymentError = lastPaymentError; }

    public OffsetDateTime getCreatedAt() { return createdAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public void setUpdatedAt(OffsetDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public OrderItem getItem() {
        return item;
    }

}
