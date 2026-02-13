package com.merdeleine.payment.entity;

import com.merdeleine.payment.enums.PaymentTxnAction;
import com.merdeleine.payment.enums.PaymentTxnResult;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "payment_txn")
public class PaymentTxn {

    @Id
    @Column(name = "id", columnDefinition = "UUID")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_id", nullable = false, foreignKey = @ForeignKey(name = "fk_payment_txn_payment"))
    private Payment payment;

    @Enumerated(EnumType.STRING)
    @Column(name = "action", nullable = false, length = 20)
    private PaymentTxnAction action;

    @Enumerated(EnumType.STRING)
    @Column(name = "result", nullable = false, length = 10)
    private PaymentTxnResult result;

    @Column(name = "raw_response", columnDefinition = "JSONB")
    private String rawResponse;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    // Constructors
    public PaymentTxn() {
    }

    public PaymentTxn(UUID id, PaymentTxnAction action, PaymentTxnResult result, String rawResponse) {
        this.id = id;
        this.action = action;
        this.result = result;
        this.rawResponse = rawResponse;
    }

    // Getters and Setters
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public Payment getPayment() {
        return payment;
    }

    public void setPayment(Payment payment) {
        this.payment = payment;
    }

    public PaymentTxnAction getAction() {
        return action;
    }

    public void setAction(PaymentTxnAction action) {
        this.action = action;
    }

    public PaymentTxnResult getResult() {
        return result;
    }

    public void setResult(PaymentTxnResult result) {
        this.result = result;
    }

    public String getRawResponse() {
        return rawResponse;
    }

    public void setRawResponse(String rawResponse) {
        this.rawResponse = rawResponse;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
