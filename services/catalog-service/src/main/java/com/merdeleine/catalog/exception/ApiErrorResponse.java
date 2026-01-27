package com.merdeleine.catalog.exception;

import java.time.OffsetDateTime;

public class ApiErrorResponse {

    private String code;
    private String message;
    private OffsetDateTime timestamp;

    public ApiErrorResponse() {}

    public ApiErrorResponse(String code, String message, OffsetDateTime timestamp) {
        this.code = code;
        this.message = message;
        this.timestamp = timestamp;
    }

    public static ApiErrorResponse of(String code, String message) {
        return new ApiErrorResponse(code, message, OffsetDateTime.now());
    }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public OffsetDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(OffsetDateTime timestamp) { this.timestamp = timestamp; }
}
