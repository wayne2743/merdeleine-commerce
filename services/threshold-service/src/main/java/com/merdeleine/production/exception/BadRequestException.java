package com.merdeleine.production.exception;

public class BadRequestException extends RuntimeException {
    public BadRequestException(String message) { super(message); }
}
