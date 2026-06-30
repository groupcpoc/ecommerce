package com.ecommerce.order_service.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ExceptionTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void testResourceNotFoundException() {
        ResourceNotFoundException ex = new ResourceNotFoundException("Not Found");
        assertEquals("Not Found", ex.getMessage());

        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRequestURI()).thenReturn("/api/orders/1");

        ResponseEntity<ErrorResponse> response = handler.handleNotFound(ex, request);
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Not Found", response.getBody().getMessage());
        assertEquals("/api/orders/1", response.getBody().getPath());
        assertEquals("Resource Not Found", response.getBody().getError());
    }

    @Test
    void testInvalidOrderStateException() {
        InvalidOrderStateException ex = new InvalidOrderStateException("Invalid State");
        assertEquals("Invalid State", ex.getMessage());

        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRequestURI()).thenReturn("/api/orders/1");

        ResponseEntity<ErrorResponse> response = handler.handleInvalidState(ex, request);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Invalid State", response.getBody().getMessage());
    }

    @Test
    void testAccessDeniedException() {
        AccessDeniedException ex = new AccessDeniedException("Access Denied");

        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRequestURI()).thenReturn("/api/orders/1");

        ResponseEntity<ErrorResponse> response = handler.handleAccessDenied(ex, request);
        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("You do not have permission to perform this action.", response.getBody().getMessage());
    }

    @Test
    void testMethodArgumentNotValidException() {
        MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
        BindingResult bindingResult = mock(BindingResult.class);
        FieldError fieldError = new FieldError("object", "field", "defaultMessage");

        when(ex.getBindingResult()).thenReturn(bindingResult);
        when(bindingResult.getAllErrors()).thenReturn(List.of(fieldError));

        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRequestURI()).thenReturn("/api/orders");

        ResponseEntity<ErrorResponse> response = handler.handleValidation(ex, request);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("One or more fields failed validation.", response.getBody().getMessage());
    }

    @Test
    void testGenericException() {
        Exception ex = new Exception("Generic error");

        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRequestURI()).thenReturn("/api/orders");

        ResponseEntity<ErrorResponse> response = handler.handleGeneric(ex, request);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("An unexpected error occurred.", response.getBody().getMessage());
    }

    @Test
    void testErrorResponse_BuilderAndGetters() {
        LocalDateTime now = LocalDateTime.now();
        ErrorResponse res = ErrorResponse.builder()
                .status(200)
                .message("OK")
                .timestamp(now)
                .build();

        assertEquals(200, res.getStatus());
        assertEquals("OK", res.getMessage());
        assertEquals(now, res.getTimestamp());
    }
}
