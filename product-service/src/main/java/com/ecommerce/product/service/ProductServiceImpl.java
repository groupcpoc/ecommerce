package com.ecommerce.product.service;

import com.ecommerce.product.dto.ProductRequest;
import com.ecommerce.product.dto.ProductResponse;
import com.ecommerce.product.entity.Product;
import com.ecommerce.product.enums.ProductStatus;
import com.ecommerce.product.exception.ProductAlreadyExistsException;
import com.ecommerce.product.exception.ProductNotFoundException;
import com.ecommerce.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private static final String PRODUCT_NOT_FOUND_WITH_ID = "Product not found with id : ";

    private final ProductRepository productRepository;

    @Override
    public List<ProductResponse> getAllProducts() {
        return productRepository.findAll()
                .stream()
                .map(this::convertToResponse)
                .toList();
    }

    @Override
    public ProductResponse getProductById(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException(PRODUCT_NOT_FOUND_WITH_ID + id));
        return convertToResponse(product);
    }

    @Override
    public List<ProductResponse> searchByKeyword(String keyword) {
        return productRepository.findAll()
                .stream()
                .filter(product ->
                        product.getName().toLowerCase().contains(keyword.toLowerCase())
                                || product.getCategory().toLowerCase().contains(keyword.toLowerCase()))
                .map(this::convertToResponse)
                .toList();
    }

    @Override
    public List<ProductResponse> searchWithFilters(String name, String category) {
        return productRepository.findAll()
                .stream()
                .filter(product ->
                        (name == null || name.isBlank()
                                || product.getName().toLowerCase().contains(name.toLowerCase()))
                                && (category == null || category.isBlank()
                                || product.getCategory().equalsIgnoreCase(category)))
                .map(this::convertToResponse)
                .toList();
    }

    @Override
    public List<String> getCategories() {
        return productRepository.findAll()
                .stream()
                .map(Product::getCategory)
                .distinct()
                .toList();
    }

    @Override
    public List<ProductResponse> getProductsByCategory(String category) {
        return productRepository.findAll()
                .stream()
                .filter(product -> product.getCategory().equalsIgnoreCase(category))
                .map(this::convertToResponse)
                .toList();
    }

    @Override
    public List<ProductResponse> getProductsByStatus(ProductStatus status) {
        return productRepository.findAll()
                .stream()
                .filter(product -> product.getStatus().equals(status))
                .map(this::convertToResponse)
                .toList();
    }

    @Override
    public ProductResponse createProduct(ProductRequest request) {
        if (productRepository.existsByName(request.getName())) {
            throw new ProductAlreadyExistsException("Product already exists with name : " + request.getName());
        }

        if (productRepository.existsBySku(request.getSku())) {
            throw new ProductAlreadyExistsException("Product already exists with SKU : " + request.getSku());
        }

        Product product = Product.builder()
                .name(request.getName())
                .description(request.getDescription())
                .price(request.getPrice())
                .quantity(request.getQuantity())
                .category(request.getCategory())
                .sku(request.getSku())
                .status(request.getStatus())
                .build();

        Product savedProduct = productRepository.save(product);
        log.info("Product created successfully with id : {}", savedProduct.getId());
        return convertToResponse(savedProduct);
    }

    @Override
    public ProductResponse updateProduct(Long id, ProductRequest request) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException(PRODUCT_NOT_FOUND_WITH_ID + id));

        Optional<Product> existingName = productRepository.findByName(request.getName());
        if (existingName.isPresent() && !existingName.get().getId().equals(id)) {
            throw new ProductAlreadyExistsException("Product already exists with name : " + request.getName());
        }

        Optional<Product> existingSku = productRepository.findBySku(request.getSku());
        if (existingSku.isPresent() && !existingSku.get().getId().equals(id)) {
            throw new ProductAlreadyExistsException("Product already exists with SKU : " + request.getSku());
        }

        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setPrice(request.getPrice());
        product.setQuantity(request.getQuantity());
        product.setCategory(request.getCategory());
        product.setSku(request.getSku());
        product.setStatus(request.getStatus());

        Product updatedProduct = productRepository.save(product);
        log.info("Product updated successfully with id : {}", updatedProduct.getId());
        return convertToResponse(updatedProduct);
    }

    @Override
    public ProductResponse updateQuantity(Long id, Integer quantity) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException(PRODUCT_NOT_FOUND_WITH_ID + id));
        product.setQuantity(quantity);
        Product updatedProduct = productRepository.save(product);
        log.info("Product quantity updated successfully for id : {}", updatedProduct.getId());
        return convertToResponse(updatedProduct);
    }

    @Override
    public void deleteProduct(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException(PRODUCT_NOT_FOUND_WITH_ID + id));
        productRepository.delete(product);
        log.info("Product deleted successfully with id : {}", id);
    }

    private ProductResponse convertToResponse(Product product) {
        return ProductResponse.builder()
                .id(product.getId())
                .name(product.getName())
                .description(product.getDescription())
                .price(product.getPrice())
                .quantity(product.getQuantity())
                .category(product.getCategory())
                .sku(product.getSku())
                .status(product.getStatus())
                .build();
    }
}