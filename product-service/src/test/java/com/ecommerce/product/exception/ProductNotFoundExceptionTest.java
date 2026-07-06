package com.ecommerce.product.exception;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ProductNotFoundExceptionTest {

    @Test
    void exceptionCanBeCreatedWithMessage() {
        String message = "Product not found";
        ProductNotFoundException exception = new ProductNotFoundException(message);

        assertEquals(message, exception.getMessage());
    }

    @Test
    void exceptionIsRuntimeException() {
        ProductNotFoundException exception = new ProductNotFoundException("test");
        assertTrue(exception instanceof RuntimeException);
    }
}