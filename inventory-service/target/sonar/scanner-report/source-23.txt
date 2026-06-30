package com.ecommerce.inventoryservice.exception;

import java.util.stream.Collectors;

import com.ecommerce.inventoryservice.dto.ApiResponse;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

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

    @ExceptionHandler(OrderStockNotFoundException.class)
    public ResponseEntity<ApiResponse<Object>> handleOrderStockNotFound(OrderStockNotFoundException ex) {
        return ResponseEntity.ok(ApiResponse.failure(ex.getMessage(), "ORDER_STOCK_NOT_FOUND", null));
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

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiResponse<Object>> handleMissingParameter(MissingServletRequestParameterException ex) {
        return ResponseEntity.ok(ApiResponse.failure(
                "Missing required request parameter: " + ex.getParameterName(),
                "VALIDATION_ERROR",
                null));
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiResponse<Object>> handleMethodNotSupported(HttpRequestMethodNotSupportedException ex) {
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
                .body(ApiResponse.failure("Method not allowed", "METHOD_NOT_ALLOWED", null));
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiResponse<Object>> handleNoResourceFound(NoResourceFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.failure("Resource not found", "RESOURCE_NOT_FOUND", null));
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
