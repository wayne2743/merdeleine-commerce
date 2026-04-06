package com.merdeleine.payment.dto;

import com.merdeleine.enums.PaymentProvider;
import com.merdeleine.enums.PaymentStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.time.OffsetDateTime;
import java.util.UUID;

public class PaymentCreateRequest {

    @NotNull
    private UUID orderId;

    @NotNull
    private PaymentProvider provider;

    private PaymentStatus status;

    @NotNull
    @Positive
    private Integer amountCents;

    @NotBlank
    @Size(max = 10)
    private String currency;

    @Size(max = 100)
    private String providerPaymentId;

    @Size(max = 100)
    private String providerCaptureId;

    @Size(max = 500)
    private String approveUrl;

    private OffsetDateTime expireAt;

    private OffsetDateTime expiredAt;

    @Pattern(regexp = "\\d{5}", message = "bankLastFive must be 5 digits")
    private String bankLastFive;

    private OffsetDateTime transferAt;

    public UUID getOrderId() {
        return orderId;
    }

    public void setOrderId(UUID orderId) {
        this.orderId = orderId;
    }

    public PaymentProvider getProvider() {
        return provider;
    }

    public void setProvider(PaymentProvider provider) {
        this.provider = provider;
    }

    public PaymentStatus getStatus() {
        return status;
    }

    public void setStatus(PaymentStatus status) {
        this.status = status;
    }

    public Integer getAmountCents() {
        return amountCents;
    }

    public void setAmountCents(Integer amountCents) {
        this.amountCents = amountCents;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public String getProviderPaymentId() {
        return providerPaymentId;
    }

    public void setProviderPaymentId(String providerPaymentId) {
        this.providerPaymentId = providerPaymentId;
    }

    public String getProviderCaptureId() {
        return providerCaptureId;
    }

    public void setProviderCaptureId(String providerCaptureId) {
        this.providerCaptureId = providerCaptureId;
    }

    public String getApproveUrl() {
        return approveUrl;
    }

    public void setApproveUrl(String approveUrl) {
        this.approveUrl = approveUrl;
    }

    public OffsetDateTime getExpireAt() {
        return expireAt;
    }

    public void setExpireAt(OffsetDateTime expireAt) {
        this.expireAt = expireAt;
    }

    public OffsetDateTime getExpiredAt() {
        return expiredAt;
    }

    public void setExpiredAt(OffsetDateTime expiredAt) {
        this.expiredAt = expiredAt;
    }

    public String getBankLastFive() {
        return bankLastFive;
    }

    public void setBankLastFive(String bankLastFive) {
        this.bankLastFive = bankLastFive;
    }

    public OffsetDateTime getTransferAt() {
        return transferAt;
    }

    public void setTransferAt(OffsetDateTime transferAt) {
        this.transferAt = transferAt;
    }
}

