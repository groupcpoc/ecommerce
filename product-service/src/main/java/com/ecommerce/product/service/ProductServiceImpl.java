package com.ecommerce.product.service;

import com.ecommerce.product.dto.ProductRequest;
import com.ecommerce.product.dto.ProductResponse;
import com.ecommerce.product.entity.Product;
import com.ecommerce.product.enums.ProductStatus;
import com.ecommerce.product.exception.ProductNotFoundException;
import com.ecommerce.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

@Service
@Slf4j
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;

    @Override
    @Transactional
    public ProductResponse createProduct(ProductRequest productRequest) {
        if (productRequest.getSku() != null &&
                productRepository.existsBySku(productRequest.getSku())) {
            log.error("SKU already exists: {}", productRequest.getSku());
            throw new IllegalArgumentException("SKU already exists: " + productRequest.getSku());
        }

        Product product = Product.builder()
                .name(productRequest.getName())
                .description(productRequest.getDescription())
                .price(productRequest.getPrice())
                .quantity(productRequest.getQuantity())
                .category(productRequest.getCategory())
                .sku(productRequest.getSku())
                .status(productRequest.getStatus() != null ?
                        productRequest.getStatus() : ProductStatus.ACTIVE)
                .build();

        Product savedProduct = productRepository.save(product);
        log.info("Product created successfully with ID: {}", savedProduct.getId());

        return mapToProductResponse(savedProduct);
    }

    @Override
    @Transactional(readOnly = true)
    public ProductResponse getProductById(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException(
                        "Product not found with ID: " + id));

        log.info("Product retrieved with ID: {}", id);
        return mapToProductResponse(product);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductResponse> getAllProducts() {
        List<Product> products = productRepository.findAll();
        log.info("Retrieved {} products", products.size());
        return products.stream()
                .map(this::mapToProductResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductResponse> getProductsByCategory(String category) {
        List<Product> products = productRepository.findByCategory(category);
        log.info("Retrieved {} products for category: {}", products.size(), category);
        return products.stream()
                .map(this::mapToProductResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductResponse> getProductsByStatus(ProductStatus status) {
        List<Product> products = productRepository.findByStatus(status);
        log.info("Retrieved {} products with status: {}", products.size(), status);
        return products.stream()
                .map(this::mapToProductResponse)
                .toList();
    }

    @Override
    @Transactional
    public ProductResponse updateProduct(Long id, ProductRequest productRequest) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException(
                        "Product not found with ID: " + id));

        if (productRequest.getSku() != null &&
                !product.getSku().equals(productRequest.getSku()) &&
                productRepository.existsBySku(productRequest.getSku())) {
            throw new IllegalArgumentException("SKU already exists: " + productRequest.getSku());
        }

        product.setName(productRequest.getName());
        product.setDescription(productRequest.getDescription());
        product.setPrice(productRequest.getPrice());
        product.setQuantity(productRequest.getQuantity());
        product.setCategory(productRequest.getCategory());
        product.setSku(productRequest.getSku());
        product.setStatus(productRequest.getStatus() != null ?
                productRequest.getStatus() : product.getStatus());

        Product updatedProduct = productRepository.save(product);
        log.info("Product updated successfully with ID: {}", id);

        return mapToProductResponse(updatedProduct);
    }

    @Override
    @Transactional
    public void deleteProduct(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException(
                        "Product not found with ID: " + id));

        productRepository.delete(product);
        log.info("Product deleted successfully with ID: {}", id);
    }

    @Override
    @Transactional
    public ProductResponse updateQuantity(Long id, Integer quantity) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException(
                        "Product not found with ID: " + id));

        product.setQuantity(quantity);

        if (quantity == 0) {
            product.setStatus(ProductStatus.OUT_OF_STOCK);
        } else if (quantity > 0 && product.getStatus() == ProductStatus.OUT_OF_STOCK) {
            product.setStatus(ProductStatus.ACTIVE);
        }

        Product updatedProduct = productRepository.save(product);
        log.info("Product quantity updated to {} for ID: {}", quantity, id);

        return mapToProductResponse(updatedProduct);
    }

    @Override
    @Transactional(readOnly = true)
    public Product getProductEntityById(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException(
                        "Product not found with ID: " + id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductResponse> searchProducts(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return getAllProducts();
        }

        List<Product> products = productRepository.searchByNameOrCategory(keyword);
        log.info("Found {} products matching keyword: {}", products.size(), keyword);

        return products.stream()
                .map(this::mapToProductResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductResponse> searchWithFilters(String name, String category) {
        List<Product> products = productRepository.searchWithFilters(name, category);
        log.info("Found {} products with filters: name={}, category={}",
                products.size(), name, category);

        return products.stream()
                .map(this::mapToProductResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Set<String> getAllCategories() {
        Set<String> categories = productRepository.findAllUniqueCategories();
        log.info("Retrieved {} unique categories", categories.size());
        return categories;
    }

    private ProductResponse mapToProductResponse(Product product) {
        return ProductResponse.builder()
                .id(product.getId())
                .name(product.getName())
                .description(product.getDescription())
                .price(product.getPrice())
                .quantity(product.getQuantity())
                .category(product.getCategory())
                .sku(product.getSku())
                .status(product.getStatus())
                .createdAt(product.getCreatedAt())
                .updatedAt(product.getUpdatedAt())
                .build();
    }
}