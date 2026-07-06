package com.ecommerce.product.dto;

import com.ecommerce.product.enums.ProductStatus;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ProductRequestValidationTest {

    private Validator validator;

    @BeforeEach
    void setUp() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    @Test
    void validRequestShouldHaveNoViolations() {
        ProductRequest request = new ProductRequest("Phone", "Smart phone", 1000.0, 10, "Electronics", "SKU-1", ProductStatus.ACTIVE);
        assertTrue(validator.validate(request).isEmpty());
    }

    @Test
    void blankNameShouldFail() {
        ProductRequest request = new ProductRequest("", "Smart phone", 1000.0, 10, "Electronics", "SKU-1", ProductStatus.ACTIVE);
        assertFalse(validator.validate(request).isEmpty());
    }

    @Test
    void nullDescriptionShouldPass() {
        ProductRequest request = new ProductRequest("Phone", null, 1000.0, 10, "Electronics", "SKU-1", ProductStatus.ACTIVE);
        assertTrue(validator.validate(request).isEmpty());
    }

    @Test
    void nullPriceShouldFail() {
        ProductRequest request = new ProductRequest("Phone", "Smart phone", null, 10, "Electronics", "SKU-1", ProductStatus.ACTIVE);
        assertFalse(validator.validate(request).isEmpty());
    }

    @Test
    void negativeQuantityShouldFail() {
        ProductRequest request = new ProductRequest("Phone", "Smart phone", 1000.0, -1, "Electronics", "SKU-1", ProductStatus.ACTIVE);
        assertFalse(validator.validate(request).isEmpty());
    }

    @Test
    void blankCategoryShouldFail() {
        ProductRequest request = new ProductRequest("Phone", "Smart phone", 1000.0, 10, "", "SKU-1", ProductStatus.ACTIVE);
        assertFalse(validator.validate(request).isEmpty());
    }

    @Test
    void blankSkuShouldFail() {
        ProductRequest request = new ProductRequest("Phone", "Smart phone", 1000.0, 10, "Electronics", "", ProductStatus.ACTIVE);
        assertFalse(validator.validate(request).isEmpty());
    }

    @Test
    void nullStatusShouldFail() {
        ProductRequest request = new ProductRequest("Phone", "Smart phone", 1000.0, 10, "Electronics", "SKU-1", null);
        assertFalse(validator.validate(request).isEmpty());
    }
}