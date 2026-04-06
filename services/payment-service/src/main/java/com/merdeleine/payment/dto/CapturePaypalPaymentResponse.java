package com.merdeleine.payment.dto;

public class CapturePaypalPaymentResponse {

    private String paymentStatus;
    private String providerPaymentId;
    private String providerCaptureId;

    public CapturePaypalPaymentResponse(String paymentStatus,
                                        String providerPaymentId,
                                        String providerCaptureId) {
        this.paymentStatus = paymentStatus;
        this.providerPaymentId = providerPaymentId;
        this.providerCaptureId = providerCaptureId;
    }

    public String getPaymentStatus() {
        return paymentStatus;
    }

    public String getProviderPaymentId() {
        return providerPaymentId;
    }

    public String getProviderCaptureId() {
        return providerCaptureId;
    }
}