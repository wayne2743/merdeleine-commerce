package com.merdeleine.payment.dto;

import com.merdeleine.enums.PaymentProvider;
import com.merdeleine.enums.PaymentStatus;

import java.time.OffsetDateTime;
import java.util.UUID;

public record PaymentWithSellWindowResponse(
        UUID id,
        UUID orderId,
        PaymentProvider provider,
        PaymentStatus status,
        Integer amountCents,
        String currency,
        String providerPaymentId,
        String providerCaptureId,
        String approveUrl,
        String bankLastFive,
        OffsetDateTime transferAt,
        OffsetDateTime expireAt,
        OffsetDateTime expiredAt,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt,
        UUID sellWindowId,
        String sellWindowName,
        UUID customerId,
        String customerName,
        String customerPhone,
        String customerEmail
) {
}

