package com.ecommerce.product.exception;

import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void handleProductNotFound_shouldReturn404() {
        ResponseEntity<Map<String, String>> response =
                handler.handleProductNotFound(new ProductNotFoundException("not found"));

        assertEquals(404, response.getStatusCode().value());
        assertEquals("not found", response.getBody().get("error"));
    }
}