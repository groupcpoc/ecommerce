package com.ecommerce.product.entity;

import com.ecommerce.product.enums.ProductStatus;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ProductTest {

    @Test
    void productCanBeCreatedWithBuilder() {
        Product product = Product.builder()
                .id(1L)
                .name("Laptop")
                .description("Gaming laptop")
                .price(100000.0)
                .quantity(5)
                .category("Electronics")
                .sku("SKU123")
                .status(ProductStatus.ACTIVE)
                .build();

        assertNotNull(product);
        assertEquals(1L, product.getId());
        assertEquals("Laptop", product.getName());
        assertEquals("Gaming laptop", product.getDescription());
    }

    @Test
    void productGettersWork() {
        Product product = Product.builder()
                .id(1L)
                .name("Phone")
                .description("Smart phone")
                .price(50000.0)
                .quantity(10)
                .category("Mobiles")
                .sku("SKU456")
                .status(ProductStatus.ACTIVE)
                .build();

        assertEquals(1L, product.getId());
        assertEquals("Phone", product.getName());
        assertEquals("Smart phone", product.getDescription());
        assertEquals(50000.0, product.getPrice());
        assertEquals(10, product.getQuantity());
        assertEquals("Mobiles", product.getCategory());
        assertEquals("SKU456", product.getSku());
        assertEquals(ProductStatus.ACTIVE, product.getStatus());
    }

    @Test
    void productSettersWork() {
        Product product = new Product();
        product.setId(2L);
        product.setName("Tablet");
        product.setDescription("Portable tablet");
        product.setPrice(30000.0);
        product.setQuantity(15);
        product.setCategory("Electronics");
        product.setSku("SKU789");
        product.setStatus(ProductStatus.INACTIVE);

        assertEquals(2L, product.getId());
        assertEquals("Tablet", product.getName());
        assertEquals("Portable tablet", product.getDescription());
        assertEquals(30000.0, product.getPrice());
        assertEquals(15, product.getQuantity());
        assertEquals("Electronics", product.getCategory());
        assertEquals("SKU789", product.getSku());
        assertEquals(ProductStatus.INACTIVE, product.getStatus());
    }

    @Test
    void productCanBeCreatedWithNoArgsConstructor() {
        Product product = new Product();
        assertNotNull(product);
    }

    @Test
    void productCanBeCreatedWithAllArgsConstructor() {
        Product product = new Product(1L, "Laptop", "Gaming", 100000.0, 5, "Electronics", "SKU123", ProductStatus.ACTIVE);
        assertEquals(1L, product.getId());
        assertEquals("Laptop", product.getName());
    }

    @Test
    void productEquality() {
        Product product1 = Product.builder().id(1L).name("Laptop").description("Gaming").price(100000.0).quantity(5).category("Electronics").sku("SKU123").status(ProductStatus.ACTIVE).build();
        Product product2 = Product.builder().id(1L).name("Laptop").description("Gaming").price(100000.0).quantity(5).category("Electronics").sku("SKU123").status(ProductStatus.ACTIVE).build();
        assertEquals(product1, product2);
    }

    @Test
    void productToString() {
        Product product = Product.builder().id(1L).name("Laptop").build();
        assertNotNull(product.toString());
        assertFalse(product.toString().isEmpty());
    }

    @Test
    void productHashCode() {
        Product product = Product.builder().id(1L).name("Laptop").build();
        assertNotEquals(0, product.hashCode());
    }

    @Test
    void productWithNullDescription() {
        Product product = Product.builder().id(1L).name("Laptop").description(null).price(100000.0).quantity(5).category("Electronics").sku("SKU123").status(ProductStatus.ACTIVE).build();
        assertNull(product.getDescription());
    }

    @Test
    void productWithDifferentStatuses() {
        for (ProductStatus status : ProductStatus.values()) {
            Product product = Product.builder().id(1L).name("Product").price(100.0).quantity(5).category("Category").sku("SKU").status(status).build();
            assertEquals(status, product.getStatus());
        }
    }
}