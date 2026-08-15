package com.merdeleine.payment.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record NewebPayRefundRequest(
        @NotNull @Positive Integer amountCents
) {
}
