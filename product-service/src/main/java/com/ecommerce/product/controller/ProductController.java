package com.ecommerce.product.controller;

import com.ecommerce.product.dto.ProductRequest;
import com.ecommerce.product.dto.ProductResponse;
import com.ecommerce.product.enums.ProductStatus;
import com.ecommerce.product.service.ProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/api/products")
@Slf4j
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    // ✅ CUSTOMER: Get all products
    @GetMapping
    public ResponseEntity<List<ProductResponse>> getAllProducts() {
        List<ProductResponse> products = productService.getAllProducts();
        log.info("Retrieved {} products", products.size());
        return ResponseEntity.ok(products);
    }

    // ✅ CUSTOMER: Get product by ID
    @GetMapping("/{id}")
    public ResponseEntity<ProductResponse> getProductById(@PathVariable Long id) {
        ProductResponse response = productService.getProductById(id);
        return ResponseEntity.ok(response);
    }

    // ✅ CUSTOMER: Search products by name OR category
    @GetMapping("/search")
    public ResponseEntity<List<ProductResponse>> searchProducts(
            @RequestParam String keyword) {
        List<ProductResponse> products = productService.searchProducts(keyword);
        log.info("Search found {} products for keyword: {}", products.size(), keyword);
        return ResponseEntity.ok(products);
    }

    // ✅ CUSTOMER: Search with name AND category filters
    @GetMapping("/search/filters")
    public ResponseEntity<List<ProductResponse>> searchWithFilters(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String category) {
        List<ProductResponse> products = productService.searchWithFilters(name, category);
        return ResponseEntity.ok(products);
    }

    // ✅ CUSTOMER: Get all categories
    @GetMapping("/categories")
    public ResponseEntity<Set<String>> getAllCategories() {
        Set<String> categories = productService.getAllCategories();
        return ResponseEntity.ok(categories);
    }

    // ✅ CUSTOMER: Get products by category
    @GetMapping("/category/{category}")
    public ResponseEntity<List<ProductResponse>> getProductsByCategory(
            @PathVariable String category) {
        List<ProductResponse> products = productService.getProductsByCategory(category);
        return ResponseEntity.ok(products);
    }

    // ✅ CUSTOMER: Get products by status
    @GetMapping("/status/{status}")
    public ResponseEntity<List<ProductResponse>> getProductsByStatus(
            @PathVariable ProductStatus status) {
        List<ProductResponse> products = productService.getProductsByStatus(status);
        return ResponseEntity.ok(products);
    }

    // ✅ ADMIN: Create product
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ProductResponse> createProduct(
            @RequestBody ProductRequest productRequest) {
        ProductResponse response = productService.createProduct(productRequest);
        log.info("Product created with ID: {}", response.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // ✅ ADMIN: Update product
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ProductResponse> updateProduct(
            @PathVariable Long id,
            @RequestBody ProductRequest productRequest) {
        ProductResponse response = productService.updateProduct(id, productRequest);
        return ResponseEntity.ok(response);
    }

    // ✅ ADMIN: Delete product
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteProduct(@PathVariable Long id) {
        productService.deleteProduct(id);
        return ResponseEntity.noContent().build();
    }

    // ✅ ADMIN: Update quantity
    @PutMapping("/{id}/quantity")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ProductResponse> updateQuantity(
            @PathVariable Long id,
            @RequestParam Integer quantity) {
        ProductResponse response = productService.updateQuantity(id, quantity);
        return ResponseEntity.ok(response);
    }
}