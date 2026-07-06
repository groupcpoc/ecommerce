package com.ecommerce.product.dto;

import com.ecommerce.product.enums.ProductStatus;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ProductRequestTest {

    @Test
    void productRequestCanBeCreatedWithNoArgsConstructor() {
        ProductRequest request = new ProductRequest();
        assertNotNull(request);
    }

    @Test
    void productRequestCanBeCreatedWithAllArgsConstructor() {
        ProductRequest request = new ProductRequest("Laptop", "Gaming", 100000.0, 5, "Electronics", "SKU123", ProductStatus.ACTIVE);
        assertEquals("Laptop", request.getName());
        assertEquals("Gaming", request.getDescription());
        assertEquals(100000.0, request.getPrice());
        assertEquals(5, request.getQuantity());
        assertEquals("Electronics", request.getCategory());
        assertEquals("SKU123", request.getSku());
        assertEquals(ProductStatus.ACTIVE, request.getStatus());
    }
}