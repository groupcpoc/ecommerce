package com.ecommerce.product.service;

import com.ecommerce.product.dto.ProductRequest;
import com.ecommerce.product.dto.ProductResponse;
import com.ecommerce.product.entity.Product;
import com.ecommerce.product.enums.ProductStatus;

import java.util.List;
import java.util.Set;

public interface ProductService {

    ProductResponse createProduct(ProductRequest productRequest);

    ProductResponse getProductById(Long id);

    List<ProductResponse> getAllProducts();

    List<ProductResponse> getProductsByCategory(String category);

    List<ProductResponse> getProductsByStatus(ProductStatus status);

    ProductResponse updateProduct(Long id, ProductRequest productRequest);

    void deleteProduct(Long id);

    ProductResponse updateQuantity(Long id, Integer quantity);

    Product getProductEntityById(Long id);

    List<ProductResponse> searchProducts(String keyword);

    List<ProductResponse> searchWithFilters(String name, String category);

    Set<String> getAllCategories();
}