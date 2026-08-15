package com.merdeleine.payment.dto;

import com.merdeleine.payment.enums.PaymentRefundStatus;

import java.time.OffsetDateTime;
import java.util.UUID;

public record NewebPayRefundResponse(
        UUID refundId,
        UUID paymentId,
        Integer amountCents,
        PaymentRefundStatus status,
        String providerCode,
        String providerMessage,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
