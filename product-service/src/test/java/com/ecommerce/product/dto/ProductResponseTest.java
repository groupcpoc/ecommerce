package com.ecommerce.product.dto;

import com.ecommerce.product.enums.ProductStatus;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ProductResponseTest {

    @Test
    void testNoArgsConstructor() {

        ProductResponse response = new ProductResponse();

        response.setId(1L);
        response.setName("Laptop");
        response.setDescription("Gaming");
        response.setPrice(50000.0);
        response.setQuantity(5);
        response.setCategory("Electronics");
        response.setSku("SKU1");
        response.setStatus(ProductStatus.ACTIVE);

        assertEquals(1L, response.getId());
        assertEquals("Laptop", response.getName());
        assertEquals("Gaming", response.getDescription());
        assertEquals(50000.0, response.getPrice());
        assertEquals(5, response.getQuantity());
        assertEquals("Electronics", response.getCategory());
        assertEquals("SKU1", response.getSku());
        assertEquals(ProductStatus.ACTIVE, response.getStatus());
    }

    @Test
    void testAllArgsConstructor() {

        ProductResponse response = new ProductResponse(
                2L,
                "Phone",
                "Android",
                25000.0,
                10,
                "Mobiles",
                "SKU2",
                ProductStatus.OUT_OF_STOCK
        );

        assertEquals(2L, response.getId());
        assertEquals("Phone", response.getName());
    }

    @Test
    void testBuilder() {

        ProductResponse response = ProductResponse.builder()
                .id(3L)
                .name("TV")
                .description("Smart")
                .price(45000.0)
                .quantity(3)
                .category("Electronics")
                .sku("SKU3")
                .status(ProductStatus.ACTIVE)
                .build();

        assertNotNull(response);
    }

    @Test
    void testEqualsHashCode() {

        ProductResponse p1 = ProductResponse.builder()
                .id(1L)
                .name("Laptop")
                .build();

        ProductResponse p2 = ProductResponse.builder()
                .id(1L)
                .name("Laptop")
                .build();

        assertEquals(p1, p2);
        assertEquals(p1.hashCode(), p2.hashCode());
    }

    @Test
    void testNotEquals() {

        ProductResponse p1 = ProductResponse.builder().id(1L).build();

        ProductResponse p2 = ProductResponse.builder().id(2L).build();

        assertNotEquals(p1, p2);
    }

    @Test
    void testToString() {

        ProductResponse response = ProductResponse.builder()
                .id(1L)
                .name("Laptop")
                .sku("SKU")
                .build();

        String value = response.toString();

        assertNotNull(value);
        assertTrue(value.contains("Laptop"));
    }

    @Test
    void testEqualsSelf() {

        ProductResponse response = new ProductResponse();

        assertEquals(response, response);
    }

    @Test
    void testEqualsNull() {

        ProductResponse response = new ProductResponse();

        assertNotEquals(null, response);
    }

    @Test
    void testEqualsDifferentClass() {

        ProductResponse response = new ProductResponse();

        assertNotEquals("ABC", response);
    }
}