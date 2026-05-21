package com.merdeleine.catalog.exception;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);


    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleNotFound(NotFoundException ex) {
        log.error(ex.getMessage(), ex);
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiErrorResponse.of("NOT_FOUND", ex.getMessage()));
    }

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ApiErrorResponse> handleBadRequest(BadRequestException ex) {
        log.error(ex.getMessage(), ex);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiErrorResponse.of("BAD_REQUEST", ex.getMessage()));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<?> handleConflict(IllegalStateException e) {
        return ResponseEntity.status(409).body(ApiErrorResponse.of("CONFLICT", e.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        log.error(ex.getMessage(), ex);
        List<ApiErrorResponse.FieldViolation> errors =
                ex.getBindingResult().getFieldErrors().stream()
                        .map(err -> new ApiErrorResponse.FieldViolation(
                                err.getField(),
                                err.getDefaultMessage()
                        ))
                        .toList();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiErrorResponse.of(
                        "VALIDATION_ERROR",
                        "Validation failed",
                        errors
                ));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiErrorResponse> handleConstraintViolation(ConstraintViolationException ex) {
        log.error(ex.getMessage(), ex);
        List<ApiErrorResponse.FieldViolation> errors = ex.getConstraintViolations().stream()
                .map(this::toFieldViolation)
                .toList();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiErrorResponse.of("VALIDATION_ERROR", "Validation failed", errors));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiErrorResponse> handleMessageNotReadable(HttpMessageNotReadableException ex) {
        log.error(ex.getMessage(), ex);

        Throwable cause = ex.getCause();
        if (cause instanceof InvalidFormatException ife) {
            String field = pathToField(ife.getPath());
            String message;
            if (ife.getTargetType() != null && ife.getTargetType().isEnum()) {
                String allowed = Arrays.stream(ife.getTargetType().getEnumConstants())
                        .map(Object::toString)
                        .collect(Collectors.joining(", "));
                message = "Invalid value '" + ife.getValue() + "' for " + field
                        + "; allowed values: [" + allowed + "]";
            } else {
                message = "Invalid value '" + ife.getValue() + "' for " + field;
            }
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiErrorResponse.of("BAD_REQUEST", message,
                            List.of(new ApiErrorResponse.FieldViolation(field, message))));
        }
        if (cause instanceof JsonMappingException jme) {
            String field = pathToField(jme.getPath());
            String message = "Malformed request body at '" + field + "': " + jme.getOriginalMessage();
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiErrorResponse.of("BAD_REQUEST", message,
                            List.of(new ApiErrorResponse.FieldViolation(field, message))));
        }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiErrorResponse.of("BAD_REQUEST", "Malformed request body"));
    }

    private String pathToField(List<JsonMappingException.Reference> path) {
        if (path == null || path.isEmpty()) return "unknown";
        StringBuilder sb = new StringBuilder();
        for (JsonMappingException.Reference ref : path) {
            if (ref.getFieldName() != null) {
                if (sb.length() > 0) sb.append('.');
                sb.append(ref.getFieldName());
            } else if (ref.getIndex() >= 0) {
                sb.append('[').append(ref.getIndex()).append(']');
            }
        }
        return sb.length() == 0 ? "unknown" : sb.toString();
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleUnexpected(Exception ex) {
        log.error("Unhandled exception", ex); // <--- 這行會把真正原因印出來
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
