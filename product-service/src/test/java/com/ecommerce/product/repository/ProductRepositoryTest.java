package com.ecommerce.product.repository;

import com.ecommerce.product.enums.ProductStatus;
import com.ecommerce.product.entity.Product;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
class ProductRepositoryTest {

    @Autowired
    private ProductRepository productRepository;

    @Test
    void saveAndFindByIdShouldWork() {
        Product product = Product.builder()
                .name("Phone")
                .description("Smart phone")
                .price(1000.0)
                .quantity(10)
                .category("Electronics")
                .sku("SKU-1")
                .status(ProductStatus.ACTIVE)
                .build();

        Product saved = productRepository.save(product);
        Optional<Product> found = productRepository.findById(saved.getId());

        assertTrue(found.isPresent());
        assertEquals("Phone", found.get().getName());
        assertEquals("SKU-1", found.get().getSku());
    }

    @Test
    void findBySkuShouldReturnProduct() {
        Product product = Product.builder()
                .name("Phone")
                .description("Smart phone")
                .price(1000.0)
                .quantity(10)
                .category("Electronics")
                .sku("SKU-2")
                .status(ProductStatus.ACTIVE)
                .build();

        productRepository.save(product);
        Optional<Product> found = productRepository.findBySku("SKU-2");

        assertTrue(found.isPresent());
        assertEquals("Phone", found.get().getName());
    }

    @Test
    void findByNameShouldReturnProduct() {
        Product product = Product.builder()
                .name("Laptop")
                .description("Work laptop")
                .price(50000.0)
                .quantity(5)
                .category("Electronics")
                .sku("SKU-3")
                .status(ProductStatus.ACTIVE)
                .build();

        productRepository.save(product);
        Optional<Product> found = productRepository.findByName("Laptop");

        assertTrue(found.isPresent());
        assertEquals("SKU-3", found.get().getSku());
    }

    @Test
    void findBySkuShouldReturnEmptyWhenMissing() {
        assertTrue(productRepository.findBySku("NOTFOUND").isEmpty());
    }

    @Test
    void findByNameShouldReturnEmptyWhenMissing() {
        assertTrue(productRepository.findByName("NonExistent").isEmpty());
    }

    @Test
    void saveMultipleProducts() {
        Product product1 = Product.builder()
                .name("Phone1")
                .description("Smart phone 1")
                .price(1000.0)
                .quantity(10)
                .category("Electronics")
                .sku("SKU-10")
                .status(ProductStatus.ACTIVE)
                .build();

        Product product2 = Product.builder()
                .name("Phone2")
                .description("Smart phone 2")
                .price(2000.0)
                .quantity(20)
                .category("Electronics")
                .sku("SKU-11")
                .status(ProductStatus.ACTIVE)
                .build();

        productRepository.save(product1);
        productRepository.save(product2);

        assertTrue(productRepository.findBySku("SKU-10").isPresent());
        assertTrue(productRepository.findBySku("SKU-11").isPresent());
    }

    @Test
    void updateProduct() {
        Product product = Product.builder()
                .name("Laptop")
                .description("Work laptop")
                .price(50000.0)
                .quantity(5)
                .category("Electronics")
                .sku("SKU-UPDATE")
                .status(ProductStatus.ACTIVE)
                .build();

        Product saved = productRepository.save(product);
        saved.setQuantity(10);
        productRepository.save(saved);

        Optional<Product> updated = productRepository.findById(saved.getId());

        assertTrue(updated.isPresent());
        assertEquals(10, updated.get().getQuantity());
    }

    @Test
    void deleteProduct() {
        Product product = Product.builder()
                .name("Laptop")
                .description("Work laptop")
                .price(50000.0)
                .quantity(5)
                .category("Electronics")
                .sku("SKU-DELETE")
                .status(ProductStatus.ACTIVE)
                .build();

        Product saved = productRepository.save(product);
        Long id = saved.getId();

        productRepository.deleteById(id);

        assertTrue(productRepository.findById(id).isEmpty());
    }
}