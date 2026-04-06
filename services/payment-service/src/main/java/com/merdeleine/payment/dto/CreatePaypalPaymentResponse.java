package com.merdeleine.payment.dto;

import java.util.UUID;

public class CreatePaypalPaymentResponse {

    private UUID paymentId;
    private String providerPaymentId;
    private String approveUrl;

    public CreatePaypalPaymentResponse(UUID paymentId,
                                       String providerPaymentId,
                                       String approveUrl) {
        this.paymentId = paymentId;
        this.providerPaymentId = providerPaymentId;
        this.approveUrl = approveUrl;
    }

    public UUID getPaymentId() {
        return paymentId;
    }

    public String getProviderPaymentId() {
        return providerPaymentId;
    }

    public String getApproveUrl() {
        return approveUrl;
    }
}