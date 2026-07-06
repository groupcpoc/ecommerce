package com.ecommerce.product.enums;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ProductStatusTest {

    @Test
    void allProductStatusValuesExist() {
        assertEquals(4, ProductStatus.values().length);
    }

    @Test
    void productStatusValueOfWorks() {
        assertEquals(ProductStatus.ACTIVE, ProductStatus.valueOf("ACTIVE"));
        assertEquals(ProductStatus.INACTIVE, ProductStatus.valueOf("INACTIVE"));
        assertEquals(ProductStatus.LOW_STOCK, ProductStatus.valueOf("LOW_STOCK"));
        assertEquals(ProductStatus.OUT_OF_STOCK, ProductStatus.valueOf("OUT_OF_STOCK"));
    }
}