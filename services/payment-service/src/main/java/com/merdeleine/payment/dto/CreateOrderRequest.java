package com.merdeleine.payment.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateOrderRequest(
        @NotBlank String amount,
        @NotBlank String currency
) {
}