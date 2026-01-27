package com.merdeleine.order.exception;

import java.time.OffsetDateTime;

public record ErrorResponse(
        String code,
        String message,
        OffsetDateTime timestamp
) {}
