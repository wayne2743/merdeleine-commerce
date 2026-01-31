package com.merdeleine.order.exception;


import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(SoldOutException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public Map<String, Object> handleSoldOut(SoldOutException ex) {
        return Map.of(
                "error", "SOLD_OUT",
                "message", ex.getMessage()
        );
    }
}
