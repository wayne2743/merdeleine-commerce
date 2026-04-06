package com.merdeleine.payment.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.time.OffsetDateTime;

public record BankTransferUpdateRequest(
        @NotNull
        @Pattern(regexp = "\\d{5}", message = "bankLastFive must be 5 digits")
        String bankLastFive,

        @NotNull
        OffsetDateTime transferAt
) {
}

