package com.merdeleine.payment.entity;


import com.merdeleine.enums.PaymentProvider;
import com.merdeleine.enums.PaymentStatus;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "payment",
        indexes = {
                @Index(name = "idx_payment_order_id", columnList = "order_id"),
                @Index(name = "idx_payment_status_created_at", columnList = "status, created_at")
        }
)
public class Payment {

    @Id
    @Column(name = "id", columnDefinition = "UUID")
    private UUID id;

    @Column(name = "order_id", nullable = false, columnDefinition = "UUID")
    private UUID orderId;

    @Enumerated(EnumType.STRING)
    @Column(name = "provider", nullable = false, length = 30)
    private PaymentProvider provider;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private PaymentStatus status;

    @Column(name = "amount_cents", nullable = false)
    private Integer amountCents;

    @Column(name = "currency", nullable = false, length = 10)
    private String currency;

    @Column(name = "provider_payment_id", length = 100)
    private String providerPaymentId;

    @Column(name = "expire_at")
    private OffsetDateTime expireAt;

    @Column(name = "expired_at")
    private OffsetDateTime expiredAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @PrePersist
    void prePersist() {
        if (id == null) id = UUID.randomUUID();
        if (status == null) status = PaymentStatus.INIT;
    }

    // getters/setters

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getOrderId() { return orderId; }
    public void setOrderId(UUID orderId) { this.orderId = orderId; }

    public PaymentProvider getProvider() { return provider; }
    public void setProvider(PaymentProvider provider) { this.provider = provider; }

    public PaymentStatus getStatus() { return status; }
    public void setStatus(PaymentStatus status) { this.status = status; }

    public Integer getAmountCents() { return amountCents; }
    public void setAmountCents(Integer amountCents) { this.amountCents = amountCents; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public String getProviderPaymentId() { return providerPaymentId; }
    public void setProviderPaymentId(String providerPaymentId) { this.providerPaymentId = providerPaymentId; }

    public OffsetDateTime getCreatedAt() { return createdAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }

    public OffsetDateTime getExpireAt() { return expireAt; }
    public void setExpireAt(OffsetDateTime expireAt) { this.expireAt = expireAt; }

    public OffsetDateTime getExpiredAt() { return expiredAt; }
    public void setExpiredAt(OffsetDateTime expiredAt) { this.expiredAt = expiredAt; }
}
