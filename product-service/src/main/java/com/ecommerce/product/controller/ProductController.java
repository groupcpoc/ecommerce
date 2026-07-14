package com.ecommerce.product.controller;

import com.ecommerce.product.dto.ProductRequest;
import com.ecommerce.product.dto.ProductResponse;
import com.ecommerce.product.enums.ProductStatus;
import com.ecommerce.product.entity.Product;
import com.ecommerce.product.service.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    // ✅ CUSTOMER: Get all products
    @GetMapping("/products")
    public ResponseEntity<List<ProductResponse>> getAllProducts() {
        log.info("Getting all products");
        return ResponseEntity.ok(productService.getAllProducts());
    }

    // ✅ CUSTOMER: Get product by ID
    @GetMapping("/products/{id}")
    public ResponseEntity<ProductResponse> getProductById(@PathVariable Long id) {
        log.info("Getting product by id: {}", id);
        return ResponseEntity.ok(productService.getProductById(id));
    }

    // ✅ CUSTOMER: Search by keyword
    @GetMapping("/products/search")
    public ResponseEntity<List<ProductResponse>> searchProducts(@RequestParam String keyword) {
        log.info("Searching products with keyword: {}", keyword);
        return ResponseEntity.ok(productService.searchByKeyword(keyword));
    }

    // ✅ CUSTOMER: Search with name AND category filters
    @GetMapping("/products/search/filters")
    public ResponseEntity<List<ProductResponse>> searchWithFilters(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String category) {
        log.info("Searching products with filters: name={}, category={}", name, category);
        return ResponseEntity.ok(productService.searchWithFilters(name, category));
    }

    // ✅ CUSTOMER: Get all categories
    @GetMapping("/products/categories")
    public ResponseEntity<List<String>> getCategories() {
        log.info("Getting all categories");
        return ResponseEntity.ok(productService.getCategories());
    }

    // ✅ CUSTOMER: Get products by category
    @GetMapping("/products/category/{category}")
    public ResponseEntity<List<ProductResponse>> getProductsByCategory(@PathVariable String category) {
        log.info("Getting products by category: {}", category);
        return ResponseEntity.ok(productService.getProductsByCategory(category));
    }

    // ✅ CUSTOMER: Get products by status
    @GetMapping("/products/status/{status}")
    public ResponseEntity<List<ProductResponse>> getProductsByStatus(@PathVariable ProductStatus status) {
        log.info("Getting products by status: {}", status);
        return ResponseEntity.ok(productService.getProductsByStatus(status));
    }

    // ✅ ADMIN: Create product
    @PostMapping("/product")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ProductResponse> createProduct(
            @Valid @RequestBody ProductRequest request) {
        ProductResponse response = productService.createProduct(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    // ✅ ADMIN: Update product
    @PutMapping("/products/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ProductResponse> updateProduct(@PathVariable Long id, @Valid @RequestBody ProductRequest request) {
        log.info("Updating product id: {}", id);
        return ResponseEntity.ok(productService.updateProduct(id, request));
    }

    // ✅ ADMIN: Delete product
    @DeleteMapping("/products/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteProduct(@PathVariable Long id) {
        log.info("Deleting product id: {}", id);
        productService.deleteProduct(id);
        return ResponseEntity.noContent().build();
    }
}