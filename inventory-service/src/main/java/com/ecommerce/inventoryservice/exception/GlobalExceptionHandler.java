package com.ecommerce.inventoryservice.exception;

import java.util.stream.Collectors;

import com.ecommerce.inventoryservice.dto.ApiResponse;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(InventoryNotFoundException.class)
    public ResponseEntity<ApiResponse<Object>> handleNotFound(InventoryNotFoundException ex) {
        return ResponseEntity.ok(ApiResponse.failure(ex.getMessage(), "INVENTORY_NOT_FOUND", null));
    }

    @ExceptionHandler(InventoryDomainException.class)
    public ResponseEntity<ApiResponse<Object>> handleDomain(InventoryDomainException ex) {
        return ResponseEntity.ok(ApiResponse.failure(ex.getMessage(), "INVENTORY_OPERATION_FAILED", null));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Object>> handleValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(fieldError -> fieldError.getField() + ": " + fieldError.getDefaultMessage())
                .collect(Collectors.joining(", "));
        return ResponseEntity.ok(ApiResponse.failure(message, "VALIDATION_ERROR", null));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<Object>> handleConstraintViolation(ConstraintViolationException ex) {
        return ResponseEntity.ok(ApiResponse.failure(ex.getMessage(), "VALIDATION_ERROR", null));
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class})
    public ResponseEntity<ApiResponse<Object>> handleBadRequest(Exception ex) {
        return ResponseEntity.ok(ApiResponse.failure("Invalid request payload", "VALIDATION_ERROR", null));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Object>> handleUnexpected(Exception ex) {
        log.error("Unexpected error occurred", ex);
        String message = ex.getMessage();
        return ResponseEntity.ok(ApiResponse.failure(
                message == null || message.isBlank() ? "Unexpected error occurred" : message,
                "UNEXPECTED_ERROR",
                null));
    }
}
