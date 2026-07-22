package com.ecommerce.product.service;

import com.ecommerce.product.dto.ProductRequest;
import com.ecommerce.product.dto.ProductResponse;
import com.ecommerce.product.entity.Product;
import com.ecommerce.product.enums.ProductStatus;
import com.ecommerce.product.exception.ProductAlreadyExistsException;
import com.ecommerce.product.exception.ProductNotFoundException;
import com.ecommerce.product.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceImplTest {

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private ProductServiceImpl productService;

    private Product product;
    private ProductRequest request;

    @BeforeEach
    void setUp() {
        product = Product.builder()
                .id(1L)
                .name("Phone")
                .description("Smart phone")
                .price(100.0)
                .quantity(5)
                .category("Electronics")
                .sku("SKU-1")
                .status(ProductStatus.ACTIVE)
                .build();

        request = new ProductRequest(
                "Phone",
                "Smart phone",
                100.0,
                5,
                "Electronics",
                "SKU-1",
                ProductStatus.ACTIVE
        );
    }

    @Test
    void getAllProducts_shouldReturnList() {
        when(productRepository.findAll()).thenReturn(List.of(product));

        List<ProductResponse> result = productService.getAllProducts();

        assertEquals(1, result.size());
        assertEquals("Phone", result.get(0).getName());
    }

    @Test
    void getProductById_shouldReturnProduct() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));

        ProductResponse result = productService.getProductById(1L);

        assertEquals(1L, result.getId());
        assertEquals("Phone", result.getName());
    }

    @Test
    void getProductById_shouldThrowWhenNotFound() {
        when(productRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(ProductNotFoundException.class, () -> productService.getProductById(1L));
    }

    @Test
    void searchByKeyword_shouldMatchName() {
        Product p = Product.builder()
                .id(2L)
                .name("Laptop")
                .description("Test")
                .price(200.0)
                .quantity(3)
                .category("Other")
                .sku("SKU-2")
                .status(ProductStatus.ACTIVE)
                .build();

        when(productRepository.findAll()).thenReturn(List.of(p));

        List<ProductResponse> result = productService.searchByKeyword("lap");

        assertEquals(1, result.size());
        assertEquals("Laptop", result.get(0).getName());
    }

    @Test
    void searchByKeyword_shouldMatchCategory() {
        Product p = Product.builder()
                .id(3L)
                .name("Tablet")
                .description("Test")
                .price(150.0)
                .quantity(2)
                .category("Electronics")
                .sku("SKU-3")
                .status(ProductStatus.ACTIVE)
                .build();

        when(productRepository.findAll()).thenReturn(List.of(p));

        List<ProductResponse> result = productService.searchByKeyword("elect");

        assertEquals(1, result.size());
        assertEquals("Tablet", result.get(0).getName());
    }

    @Test
    void searchByKeyword_shouldReturnEmptyWhenNoMatch() {
        Product p = Product.builder()
                .id(4L)
                .name("Shirt")
                .description("Test")
                .price(50.0)
                .quantity(10)
                .category("Clothing")
                .sku("SKU-4")
                .status(ProductStatus.ACTIVE)
                .build();

        when(productRepository.findAll()).thenReturn(List.of(p));

        List<ProductResponse> result = productService.searchByKeyword("phone");

        assertTrue(result.isEmpty());
    }

    @Test
    void searchWithFilters_shouldReturnWhenBothMatch() {
        when(productRepository.findAll()).thenReturn(List.of(product));

        List<ProductResponse> result = productService.searchWithFilters("Phone", "Electronics");

        assertEquals(1, result.size());
        assertEquals("Phone", result.get(0).getName());
    }

    @ParameterizedTest
    @MethodSource("searchWithFiltersArguments")
    void searchWithFilters_shouldReturnWhenOneFilterMissing(String name, String category) {
        when(productRepository.findAll()).thenReturn(List.of(product));

        List<ProductResponse> result = productService.searchWithFilters(name, category);

        assertEquals(1, result.size());
        assertEquals("Phone", result.get(0).getName());
    }

    static Stream<Arguments> searchWithFiltersArguments() {
        return Stream.of(
                Arguments.of(null, "Electronics"),
                Arguments.of("   ", "Electronics"),
                Arguments.of("Phone", null),
                Arguments.of("Phone", "   ")
        );
    }

    @Test
    void searchWithFilters_shouldReturnEmptyWhenNoMatch() {
        Product p = Product.builder()
                .id(3L)
                .name("Shirt")
                .description("Test")
                .price(50.0)
                .quantity(8)
                .category("Clothing")
                .sku("SKU-3")
                .status(ProductStatus.ACTIVE)
                .build();

        when(productRepository.findAll()).thenReturn(List.of(p));

        List<ProductResponse> result = productService.searchWithFilters("phone", "Electronics");

        assertTrue(result.isEmpty());
    }

    @Test
    void getCategories_shouldReturnDistinctCategories() {
        Product p2 = Product.builder()
                .id(2L)
                .name("Tablet")
                .description("Another")
                .price(150.0)
                .quantity(2)
                .category("Electronics")
                .sku("SKU-2")
                .status(ProductStatus.ACTIVE)
                .build();

        when(productRepository.findAll()).thenReturn(List.of(product, p2));

        List<String> result = productService.getCategories();

        assertEquals(1, result.size());
        assertEquals("Electronics", result.get(0));
    }

    @Test
    void getProductsByCategory_shouldReturnMatchingProducts() {
        when(productRepository.findAll()).thenReturn(List.of(product));

        List<ProductResponse> result = productService.getProductsByCategory("Electronics");

        assertEquals(1, result.size());
        assertEquals("Phone", result.get(0).getName());
    }

    @Test
    void getProductsByCategory_shouldReturnEmptyWhenNoMatch() {
        when(productRepository.findAll()).thenReturn(List.of(product));

        List<ProductResponse> result = productService.getProductsByCategory("Clothing");

        assertTrue(result.isEmpty());
    }

    @Test
    void getProductsByStatus_shouldReturnMatchingProducts() {
        when(productRepository.findAll()).thenReturn(List.of(product));

        List<ProductResponse> result = productService.getProductsByStatus(ProductStatus.ACTIVE);

        assertEquals(1, result.size());
        assertEquals("Phone", result.get(0).getName());
    }

    @Test
    void getProductsByStatus_shouldReturnEmptyWhenNoMatch() {
        Product p = Product.builder()
                .id(2L)
                .name("Laptop")
                .description("Test")
                .price(200.0)
                .quantity(3)
                .category("Electronics")
                .sku("SKU-2")
                .status(ProductStatus.INACTIVE)
                .build();

        when(productRepository.findAll()).thenReturn(List.of(p));

        List<ProductResponse> result = productService.getProductsByStatus(ProductStatus.ACTIVE);

        assertTrue(result.isEmpty());
    }

    @Test
    void createProduct_shouldSaveAndReturnResponse() {
        when(productRepository.existsByName("Phone")).thenReturn(false);
        when(productRepository.existsBySku("SKU-1")).thenReturn(false);
        when(productRepository.save(any(Product.class))).thenReturn(product);

        ProductResponse result = productService.createProduct(request);

        assertEquals("Phone", result.getName());
        assertEquals("SKU-1", result.getSku());
        verify(productRepository).save(any(Product.class));
    }

    @Test
    void createProduct_shouldThrowWhenNameExists() {
        when(productRepository.existsByName("Phone")).thenReturn(true);

        assertThrows(ProductAlreadyExistsException.class, () -> productService.createProduct(request));
        verify(productRepository, never()).existsBySku(any());
        verify(productRepository, never()).save(any());
    }

    @Test
    void createProduct_shouldThrowWhenSkuExists() {
        when(productRepository.existsByName("Phone")).thenReturn(false);
        when(productRepository.existsBySku("SKU-1")).thenReturn(true);

        assertThrows(ProductAlreadyExistsException.class, () -> productService.createProduct(request));
        verify(productRepository, never()).save(any());
    }

    @Test
    void updateProduct_shouldUpdateAndReturnResponse() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(productRepository.findByName("Phone")).thenReturn(Optional.of(product));
        when(productRepository.findBySku("SKU-1")).thenReturn(Optional.of(product));
        when(productRepository.save(any(Product.class))).thenAnswer(inv -> inv.getArgument(0));

        ProductResponse result = productService.updateProduct(1L, request);

        assertEquals("Phone", result.getName());
        assertEquals(5, result.getQuantity());
    }

    @Test
    void updateProduct_shouldAllowSameNameSameId() {
        Product sameIdName = Product.builder().id(1L).name("Phone").build();

        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(productRepository.findByName("Phone")).thenReturn(Optional.of(sameIdName));
        when(productRepository.findBySku("SKU-1")).thenReturn(Optional.of(product));
        when(productRepository.save(any(Product.class))).thenAnswer(inv -> inv.getArgument(0));

        ProductResponse result = productService.updateProduct(1L, request);

        assertEquals("Phone", result.getName());
    }

    @Test
    void updateProduct_shouldAllowSameSkuSameId() {
        Product sameIdSku = Product.builder().id(1L).sku("SKU-1").build();

        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(productRepository.findByName("Phone")).thenReturn(Optional.of(product));
        when(productRepository.findBySku("SKU-1")).thenReturn(Optional.of(sameIdSku));
        when(productRepository.save(any(Product.class))).thenAnswer(inv -> inv.getArgument(0));

        ProductResponse result = productService.updateProduct(1L, request);

        assertEquals("SKU-1", result.getSku());
    }

    @Test
    void updateProduct_shouldThrowWhenNotFound() {
        when(productRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(ProductNotFoundException.class, () -> productService.updateProduct(1L, request));
    }

    @Test
    void updateProduct_shouldThrowWhenDuplicateName() {
        Product another = Product.builder().id(2L).name("Phone").build();
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(productRepository.findByName("Phone")).thenReturn(Optional.of(another));

        assertThrows(ProductAlreadyExistsException.class, () -> productService.updateProduct(1L, request));
    }

    @Test
    void updateProduct_shouldThrowWhenDuplicateSku() {
        Product another = Product.builder().id(2L).sku("SKU-1").build();
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(productRepository.findByName("Phone")).thenReturn(Optional.of(product));
        when(productRepository.findBySku("SKU-1")).thenReturn(Optional.of(another));

        assertThrows(ProductAlreadyExistsException.class, () -> productService.updateProduct(1L, request));
    }

    @Test
    void updateQuantity_shouldUpdateQuantity() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(productRepository.save(any(Product.class))).thenAnswer(inv -> inv.getArgument(0));

        ProductResponse result = productService.updateQuantity(1L, 20);

        assertEquals(20, result.getQuantity());
    }

    @Test
    void updateQuantity_shouldThrowWhenNotFound() {
        when(productRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(ProductNotFoundException.class, () -> productService.updateQuantity(1L, 20));
    }

    @Test
    void deleteProduct_shouldThrowWhenNotFound() {
        when(productRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(ProductNotFoundException.class, () -> productService.deleteProduct(1L));
    }

    @Test
    void getAllProducts_shouldReturnEmptyList() {
        when(productRepository.findAll()).thenReturn(List.of());

        List<ProductResponse> result = productService.getAllProducts();

        assertTrue(result.isEmpty());
    }

    @Test
    void getCategories_shouldReturnEmptyListWhenNoProducts() {
        when(productRepository.findAll()).thenReturn(List.of());

        List<String> result = productService.getCategories();

        assertTrue(result.isEmpty());
    }

    @Test
    void searchByKeyword_shouldReturnEmptyList() {
        when(productRepository.findAll()).thenReturn(List.of());

        List<ProductResponse> result = productService.searchByKeyword("test");

        assertTrue(result.isEmpty());
    }

    @Test
    void searchByKeyword_shouldMatchCategoryPartially() {
        Product p = Product.builder()
                .id(5L)
                .name("Generic Product")
                .description("Premium Case")
                .price(25.0)
                .quantity(100)
                .category("Phone Accessories")
                .sku("SKU-5")
                .status(ProductStatus.ACTIVE)
                .build();

        when(productRepository.findAll()).thenReturn(List.of(p));

        List<ProductResponse> result = productService.searchByKeyword("phone");

        assertEquals(1, result.size());
        assertEquals("Generic Product", result.get(0).getName());
    }

    @Test
    void searchByKeyword_shouldBeCaseInsensitive() {
        Product p = Product.builder()
                .id(6L)
                .name("LAPTOP")
                .description("Test")
                .price(800.0)
                .quantity(2)
                .category("ELECTRONICS")
                .sku("SKU-6")
                .status(ProductStatus.ACTIVE)
                .build();

        when(productRepository.findAll()).thenReturn(List.of(p));

        List<ProductResponse> result = productService.searchByKeyword("laptop");

        assertEquals(1, result.size());
    }

    @Test
    void searchWithFilters_shouldFilterByBothNameAndCategory() {
        Product p1 = Product.builder()
                .id(7L)
                .name("Apple iPhone")
                .description("Test")
                .price(999.0)
                .quantity(10)
                .category("Electronics")
                .sku("SKU-7")
                .status(ProductStatus.ACTIVE)
                .build();

        Product p2 = Product.builder()
                .id(8L)
                .name("Samsung Phone")
                .description("Test")
                .price(899.0)
                .quantity(8)
                .category("Clothing")
                .sku("SKU-8")
                .status(ProductStatus.ACTIVE)
                .build();

        when(productRepository.findAll()).thenReturn(List.of(p1, p2));

        List<ProductResponse> result = productService.searchWithFilters("iPhone", "Electronics");

        assertEquals(1, result.size());
        assertEquals("Apple iPhone", result.get(0).getName());
    }

    @Test
    void searchWithFilters_shouldBeCaseInsensitiveForCategory() {
        Product p = Product.builder()
                .id(9L)
                .name("Product")
                .description("Test")
                .price(50.0)
                .quantity(5)
                .category("ELECTRONICS")
                .sku("SKU-9")
                .status(ProductStatus.ACTIVE)
                .build();

        when(productRepository.findAll()).thenReturn(List.of(p));

        List<ProductResponse> result = productService.searchWithFilters(null, "electronics");

        assertEquals(1, result.size());
    }

    @Test
    void searchWithFilters_shouldReturnEmptyWhenBothFiltersNoMatch() {
        when(productRepository.findAll()).thenReturn(List.of(product));

        List<ProductResponse> result = productService.searchWithFilters("Laptop", "Clothing");

        assertTrue(result.isEmpty());
    }

    @Test
    void getProductsByCategory_shouldBeCaseInsensitive() {
        Product p = Product.builder()
                .id(10L)
                .name("Test Product")
                .description("Test")
                .price(100.0)
                .quantity(5)
                .category("ELECTRONICS")
                .sku("SKU-10")
                .status(ProductStatus.ACTIVE)
                .build();

        when(productRepository.findAll()).thenReturn(List.of(p));

        List<ProductResponse> result = productService.getProductsByCategory("electronics");

        assertEquals(1, result.size());
    }

    @Test
    void getProductsByCategory_shouldReturnMultipleProducts() {
        Product p1 = product;
        Product p2 = Product.builder()
                .id(11L)
                .name("Tablet")
                .description("Test")
                .price(300.0)
                .quantity(5)
                .category("Electronics")
                .sku("SKU-11")
                .status(ProductStatus.ACTIVE)
                .build();

        when(productRepository.findAll()).thenReturn(List.of(p1, p2));

        List<ProductResponse> result = productService.getProductsByCategory("Electronics");

        assertEquals(2, result.size());
    }

    @Test
    void getProductsByStatus_shouldReturnMultipleProducts() {
        Product p1 = product;
        Product p2 = Product.builder()
                .id(12L)
                .name("Headphones")
                .description("Test")
                .price(80.0)
                .quantity(15)
                .category("Electronics")
                .sku("SKU-12")
                .status(ProductStatus.ACTIVE)
                .build();

        when(productRepository.findAll()).thenReturn(List.of(p1, p2));

        List<ProductResponse> result = productService.getProductsByStatus(ProductStatus.ACTIVE);

        assertEquals(2, result.size());
    }

    @Test
    void createProduct_shouldLogSuccessfully() {
        when(productRepository.existsByName("Phone")).thenReturn(false);
        when(productRepository.existsBySku("SKU-1")).thenReturn(false);
        when(productRepository.save(any(Product.class))).thenReturn(product);

        ProductResponse result = productService.createProduct(request);

        assertNotNull(result);
        assertEquals("Phone", result.getName());
    }

    @Test
    void updateProduct_shouldLogSuccessfully() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(productRepository.findByName("Phone")).thenReturn(Optional.of(product));
        when(productRepository.findBySku("SKU-1")).thenReturn(Optional.of(product));
        when(productRepository.save(any(Product.class))).thenAnswer(inv -> inv.getArgument(0));

        ProductResponse result = productService.updateProduct(1L, request);

        assertNotNull(result);
    }

    @Test
    void updateQuantity_shouldLogSuccessfully() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(productRepository.save(any(Product.class))).thenAnswer(inv -> inv.getArgument(0));

        ProductResponse result = productService.updateQuantity(1L, 25);

        assertEquals(25, result.getQuantity());
    }

    @Test
    void deleteProduct_shouldLogSuccessfully() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));

        productService.deleteProduct(1L);

        verify(productRepository).delete(product);
    }

    @Test
    void getProductsByStatus_shouldFilterByInactiveStatus() {
        Product p = Product.builder()
                .id(13L)
                .name("Discontinued Product")
                .description("Test")
                .price(50.0)
                .quantity(0)
                .category("Electronics")
                .sku("SKU-13")
                .status(ProductStatus.INACTIVE)
                .build();

        when(productRepository.findAll()).thenReturn(List.of(p));

        List<ProductResponse> result = productService.getProductsByStatus(ProductStatus.INACTIVE);

        assertEquals(1, result.size());
        assertEquals(ProductStatus.INACTIVE, result.get(0).getStatus());
    }

    @Test
    void searchByKeyword_shouldHandleMultipleMatches() {
        Product p1 = Product.builder()
                .id(14L)
                .name("Laptop Bag")
                .description("Test")
                .price(30.0)
                .quantity(20)
                .category("Electronics")
                .sku("SKU-14")
                .status(ProductStatus.ACTIVE)
                .build();

        Product p2 = Product.builder()
                .id(15L)
                .name("Gaming Laptop")
                .description("Test")
                .price(1200.0)
                .quantity(5)
                .category("Computers")
                .sku("SKU-15")
                .status(ProductStatus.ACTIVE)
                .build();

        when(productRepository.findAll()).thenReturn(List.of(p1, p2));

        List<ProductResponse> result = productService.searchByKeyword("laptop");

        assertEquals(2, result.size());
    }

    @Test
    void searchWithFilters_shouldHandleEmptyBothFilters() {
        when(productRepository.findAll()).thenReturn(List.of(product));

        List<ProductResponse> result = productService.searchWithFilters(null, null);

        assertEquals(1, result.size());
    }

    @Test
    void updateProduct_shouldUpdateAllFields() {
        ProductRequest updateRequest = new ProductRequest(
                "Updated Phone",
                "Updated Description",
                150.0,
                10,
                "Gadgets",
                "SKU-UPDATED",
                ProductStatus.INACTIVE
        );

        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(productRepository.findByName("Updated Phone")).thenReturn(Optional.empty());
        when(productRepository.findBySku("SKU-UPDATED")).thenReturn(Optional.empty());
        when(productRepository.save(any(Product.class))).thenAnswer(inv -> {
            Product p = inv.getArgument(0);
            return Product.builder()
                    .id(p.getId())
                    .name(p.getName())
                    .description(p.getDescription())
                    .price(p.getPrice())
                    .quantity(p.getQuantity())
                    .category(p.getCategory())
                    .sku(p.getSku())
                    .status(p.getStatus())
                    .build();
        });

        ProductResponse result = productService.updateProduct(1L, updateRequest);

        assertEquals("Updated Phone", result.getName());
        assertEquals("Updated Description", result.getDescription());
        assertEquals(150.0, result.getPrice());
        assertEquals(10, result.getQuantity());
        assertEquals("Gadgets", result.getCategory());
        assertEquals("SKU-UPDATED", result.getSku());
        assertEquals(ProductStatus.INACTIVE, result.getStatus());
    }
}