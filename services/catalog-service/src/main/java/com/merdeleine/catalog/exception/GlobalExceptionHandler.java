package com.merdeleine.catalog.exception;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandler {



    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleNotFound(NotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiErrorResponse.of("NOT_FOUND", ex.getMessage()));
    }

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ApiErrorResponse> handleBadRequest(BadRequestException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiErrorResponse.of("BAD_REQUEST", ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidation(MethodArgumentNotValidException ex) {

        List<ApiErrorResponse.FieldViolation> errors =
                ex.getBindingResult().getFieldErrors().stream()
                        .map(err -> new ApiErrorResponse.FieldViolation(err.getField(), err.getDefaultMessage()))
                        .toList();

        // 如果你也有 class-level 的 @Valid 驗證（例如 A <= B）
        List<ApiErrorResponse.FieldViolation> globalErrors =
                ex.getBindingResult().getGlobalErrors().stream()
                        .map(err -> new ApiErrorResponse.FieldViolation(err.getObjectName(), err.getDefaultMessage()))
                        .toList();

        List<ApiErrorResponse.FieldViolation> all = new java.util.ArrayList<>(errors);
        all.addAll(globalErrors);

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiErrorResponse.of("VALIDATION_ERROR", "Validation failed", all));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiErrorResponse> handleConstraintViolation(ConstraintViolationException ex) {

        List<ApiErrorResponse.FieldViolation> errors = ex.getConstraintViolations().stream()
                .map(this::toFieldViolation)
                .toList();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiErrorResponse.of("VALIDATION_ERROR", "Validation failed", errors));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleUnexpected(Exception ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiErrorResponse.of("INTERNAL_ERROR", "Unexpected error"));
    }


    private ApiErrorResponse.FieldViolation toFieldViolation(ConstraintViolation<?> v) {
        // propertyPath 例：create.arg0.thresholdQty / getById.id / request.page
        String path = v.getPropertyPath() == null ? "" : v.getPropertyPath().toString();
        String field = simplifyPropertyPath(path);

        return new ApiErrorResponse.FieldViolation(field, v.getMessage());
    }

    /**
     * 把 "create.req.thresholdQty" 或 "create.arg0.thresholdQty" 這種路徑簡化成 "thresholdQty"
     * 也能處理 "getById.id" -> "id"
     */
    private String simplifyPropertyPath(String path) {
        if (path == null || path.isBlank()) return "unknown";

        int lastDot = path.lastIndexOf('.');
        if (lastDot >= 0 && lastDot < path.length() - 1) {
            return path.substring(lastDot + 1);
        }
        return path;
    }
}
