package com.merdeleine.payment.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public class CreatePaypalPaymentRequest {

    @NotNull
    private UUID orderId;

    public UUID getOrderId() {
        return orderId;
    }

    public void setOrderId(UUID orderId) {
        this.orderId = orderId;
    }
}