package com.merdeleine.catalog.exception;

import java.time.OffsetDateTime;
import java.util.List;

public class ApiErrorResponse {

    private String code;
    private String message;
    private OffsetDateTime timestamp;
    private List<FieldViolation> errors;

    public ApiErrorResponse() {}

    public static ApiErrorResponse of(String code, String message, List<FieldViolation> errors) {
        ApiErrorResponse r = new ApiErrorResponse();
        r.code = code;
        r.message = message;
        r.timestamp = OffsetDateTime.now();
        r.errors = errors;
        return r;
    }

    public static ApiErrorResponse of(String code, String message) {
        ApiErrorResponse r = new ApiErrorResponse();
        r.code = code;
        r.message = message;
        r.timestamp = OffsetDateTime.now();
        return r;
    }

    public record FieldViolation(String field, String message) {}
}
