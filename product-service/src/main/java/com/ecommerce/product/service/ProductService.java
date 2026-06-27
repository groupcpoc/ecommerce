package com.ecommerce.product.service;

import com.ecommerce.product.dto.ProductRequest;
import com.ecommerce.product.dto.ProductResponse;
import com.ecommerce.product.enums.ProductStatus;
import java.util.List;

public interface ProductService {
    List<ProductResponse> getAllProducts();
    ProductResponse getProductById(Long id);
    List<ProductResponse> searchByKeyword(String keyword);
    List<ProductResponse> searchWithFilters(String name, String category);
    List<String> getCategories();
    List<ProductResponse> getProductsByCategory(String category);
    List<ProductResponse> getProductsByStatus(ProductStatus status);
    ProductResponse createProduct(ProductRequest request);
    ProductResponse updateProduct(Long id, ProductRequest request);
    ProductResponse updateQuantity(Long id, Integer quantity);
    void deleteProduct(Long id);
}