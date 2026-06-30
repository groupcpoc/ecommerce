package com.ecommerce.product.exception;

import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.*;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void handleProductNotFoundShouldReturn404() {
        ResponseEntity<?> response = handler.handleProductNotFound(
                new ProductNotFoundException("Product not found with id: 1")
        );

        assertEquals(404, response.getStatusCode().value());
        assertNotNull(response.getBody());
    }
}