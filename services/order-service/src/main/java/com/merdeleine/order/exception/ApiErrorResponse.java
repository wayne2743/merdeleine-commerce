package com.merdeleine.order.exception;

import java.time.OffsetDateTime;
import java.util.List;

public record ApiErrorResponse(
        String code,
        String message,
        OffsetDateTime timestamp,
        List<FieldViolation> errors
) {
    public static ApiErrorResponse of(String code, String message) {
        return new ApiErrorResponse(code, message, OffsetDateTime.now(), null);
    }

    public static ApiErrorResponse of(String code, String message, List<FieldViolation> errors) {
        return new ApiErrorResponse(code, message, OffsetDateTime.now(), errors);
    }

    public record FieldViolation(String field, String message) {}
}
