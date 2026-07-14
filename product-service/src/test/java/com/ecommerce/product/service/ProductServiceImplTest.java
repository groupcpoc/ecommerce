package com.ecommerce.product.service;

import com.ecommerce.product.dto.ProductRequest;
import com.ecommerce.product.dto.ProductResponse;
import com.ecommerce.product.entity.Product;
import com.ecommerce.product.enums.ProductStatus;
import com.ecommerce.product.exception.ProductNotFoundException;
import com.ecommerce.product.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

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
                .name("Laptop")
                .description("Gaming laptop")
                .price(50000.0)
                .quantity(5)
                .category("Electronics")
                .sku("SKU123")
                .status(ProductStatus.ACTIVE)
                .build();

        request = new ProductRequest(
                "Phone",
                "Smart phone",
                20000.0,
                10,
                "Mobiles",
                "SKU456",
                ProductStatus.LOW_STOCK
        );
    }

    @Test
    void getAllProducts_shouldReturnMappedList() {
        when(productRepository.findAll()).thenReturn(List.of(product));

        List<ProductResponse> result = productService.getAllProducts();

        assertEquals(1, result.size());
        assertEquals(1L, result.get(0).getId());
        assertEquals("Laptop", result.get(0).getName());
    }

    @Test
    void getAllProducts_shouldReturnEmptyList() {
        when(productRepository.findAll()).thenReturn(List.of());

        assertTrue(productService.getAllProducts().isEmpty());
    }

    @Test
    void getProductById_shouldReturnProductWhenFound() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));

        ProductResponse result = productService.getProductById(1L);

        assertEquals(1L, result.getId());
        assertEquals("Laptop", result.getName());
        assertEquals("Gaming laptop", result.getDescription());
    }

    @Test
    void getProductById_shouldThrowWhenNotFound() {
        when(productRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(ProductNotFoundException.class, () -> productService.getProductById(1L));
    }

    @Test
    void searchByKeyword_shouldMatchName() {
        Product p1 = Product.builder().id(1L).name("Laptop").category("Electronics").price(1.0).quantity(1).sku("S1").status(ProductStatus.ACTIVE).build();
        Product p2 = Product.builder().id(2L).name("Phone").category("Mobiles").price(1.0).quantity(1).sku("S2").status(ProductStatus.ACTIVE).build();
        when(productRepository.findAll()).thenReturn(List.of(p1, p2));

        List<ProductResponse> result = productService.searchByKeyword("lap");

        assertEquals(1, result.size());
        assertEquals("Laptop", result.get(0).getName());
    }

    @Test
    void searchByKeyword_shouldMatchCategory() {
        Product p1 = Product.builder().id(1L).name("Tablet").category("Electronics").price(1.0).quantity(1).sku("S1").status(ProductStatus.ACTIVE).build();
        when(productRepository.findAll()).thenReturn(List.of(p1));

        List<ProductResponse> result = productService.searchByKeyword("electron");

        assertEquals(1, result.size());
        assertEquals("Tablet", result.get(0).getName());
    }

    @Test
    void searchByKeyword_shouldReturnEmptyWhenNoMatch() {
        when(productRepository.findAll()).thenReturn(List.of(product));

        assertTrue(productService.searchByKeyword("xyz").isEmpty());
    }

    @Test
    void searchByKeyword_shouldBeCaseInsensitive() {
        Product p1 = Product.builder().id(1L).name("LAPTOP").category("ELECTRONICS").price(1.0).quantity(1).sku("S1").status(ProductStatus.ACTIVE).build();
        when(productRepository.findAll()).thenReturn(List.of(p1));

        assertEquals(1, productService.searchByKeyword("laptop").size());
    }

    @Test
    void searchWithFilters_shouldMatchBothFilters() {
        Product p1 = Product.builder().id(1L).name("Laptop").category("Electronics").price(1.0).quantity(1).sku("S1").status(ProductStatus.ACTIVE).build();
        Product p2 = Product.builder().id(2L).name("Phone").category("Mobiles").price(1.0).quantity(1).sku("S2").status(ProductStatus.ACTIVE).build();
        when(productRepository.findAll()).thenReturn(List.of(p1, p2));

        List<ProductResponse> result = productService.searchWithFilters("lap", "Electronics");

        assertEquals(1, result.size());
        assertEquals("Laptop", result.get(0).getName());
    }

    @Test
    void searchWithFilters_shouldSupportNameOnly() {
        Product p1 = Product.builder().id(1L).name("Laptop").category("Electronics").price(1.0).quantity(1).sku("S1").status(ProductStatus.ACTIVE).build();
        Product p2 = Product.builder().id(2L).name("Phone").category("Mobiles").price(1.0).quantity(1).sku("S2").status(ProductStatus.ACTIVE).build();
        when(productRepository.findAll()).thenReturn(List.of(p1, p2));

        assertEquals(1, productService.searchWithFilters("lap", null).size());
    }

    @Test
    void searchWithFilters_shouldSupportCategoryOnly() {
        Product p1 = Product.builder().id(1L).name("Laptop").category("Electronics").price(1.0).quantity(1).sku("S1").status(ProductStatus.ACTIVE).build();
        Product p2 = Product.builder().id(2L).name("Phone").category("Mobiles").price(1.0).quantity(1).sku("S2").status(ProductStatus.ACTIVE).build();
        when(productRepository.findAll()).thenReturn(List.of(p1, p2));

        List<ProductResponse> result = productService.searchWithFilters(null, "Mobiles");

        assertEquals(1, result.size());
        assertEquals("Phone", result.get(0).getName());
    }

    @Test
    void searchWithFilters_shouldReturnAllWhenInputsNull() {
        Product p1 = Product.builder().id(1L).name("Laptop").category("Electronics").price(1.0).quantity(1).sku("S1").status(ProductStatus.ACTIVE).build();
        Product p2 = Product.builder().id(2L).name("Phone").category("Mobiles").price(1.0).quantity(1).sku("S2").status(ProductStatus.ACTIVE).build();
        when(productRepository.findAll()).thenReturn(List.of(p1, p2));

        assertEquals(2, productService.searchWithFilters(null, null).size());
    }

    @Test
    void searchWithFilters_shouldReturnAllWhenInputsEmpty() {
        Product p1 = Product.builder().id(1L).name("Laptop").category("Electronics").price(1.0).quantity(1).sku("S1").status(ProductStatus.ACTIVE).build();
        Product p2 = Product.builder().id(2L).name("Phone").category("Mobiles").price(1.0).quantity(1).sku("S2").status(ProductStatus.ACTIVE).build();
        when(productRepository.findAll()).thenReturn(List.of(p1, p2));

        assertEquals(2, productService.searchWithFilters("", "").size());
    }

    @Test
    void getCategories_shouldReturnDistinctCategories() {
        Product p1 = Product.builder().id(1L).name("A").category("Electronics").price(1.0).quantity(1).sku("S1").status(ProductStatus.ACTIVE).build();
        Product p2 = Product.builder().id(2L).name("B").category("Mobiles").price(1.0).quantity(1).sku("S2").status(ProductStatus.ACTIVE).build();
        Product p3 = Product.builder().id(3L).name("C").category("Electronics").price(1.0).quantity(1).sku("S3").status(ProductStatus.ACTIVE).build();
        when(productRepository.findAll()).thenReturn(List.of(p1, p2, p3));

        List<String> categories = productService.getCategories();

        assertEquals(2, categories.size());
        assertTrue(categories.contains("Electronics"));
        assertTrue(categories.contains("Mobiles"));
    }

    @Test
    void getCategories_shouldReturnEmptyWhenNoProducts() {
        when(productRepository.findAll()).thenReturn(List.of());

        assertTrue(productService.getCategories().isEmpty());
    }

    @Test
    void getProductsByCategory_shouldReturnMatches() {
        Product p1 = Product.builder().id(1L).name("Laptop").category("Electronics").price(1.0).quantity(1).sku("S1").status(ProductStatus.ACTIVE).build();
        Product p2 = Product.builder().id(2L).name("Phone").category("Mobiles").price(1.0).quantity(1).sku("S2").status(ProductStatus.ACTIVE).build();
        when(productRepository.findAll()).thenReturn(List.of(p1, p2));

        List<ProductResponse> result = productService.getProductsByCategory("Electronics");

        assertEquals(1, result.size());
        assertEquals("Laptop", result.get(0).getName());
    }

    @Test
    void getProductsByCategory_shouldReturnEmptyWhenNoMatch() {
        when(productRepository.findAll()).thenReturn(List.of(product));

        assertTrue(productService.getProductsByCategory("Furniture").isEmpty());
    }

    @Test
    void getProductsByStatus_shouldReturnMatches() {
        Product p1 = Product.builder().id(1L).name("Laptop").category("Electronics").price(1.0).quantity(1).sku("S1").status(ProductStatus.ACTIVE).build();
        Product p2 = Product.builder().id(2L).name("Phone").category("Mobiles").price(1.0).quantity(1).sku("S2").status(ProductStatus.INACTIVE).build();
        when(productRepository.findAll()).thenReturn(List.of(p1, p2));

        List<ProductResponse> result = productService.getProductsByStatus(ProductStatus.ACTIVE);

        assertEquals(1, result.size());
        assertEquals("Laptop", result.get(0).getName());
    }

    @Test
    void getProductsByStatus_shouldReturnEmptyWhenNoMatch() {
        when(productRepository.findAll()).thenReturn(List.of(product));

        assertTrue(productService.getProductsByStatus(ProductStatus.INACTIVE).isEmpty());
    }

    @Test
    void createProduct_shouldSaveAndReturnResponse() {
        Product saved = Product.builder()
                .id(10L)
                .name(request.getName())
                .description(request.getDescription())
                .price(request.getPrice())
                .quantity(request.getQuantity())
                .category(request.getCategory())
                .sku(request.getSku())
                .status(request.getStatus())
                .build();

        when(productRepository.save(any(Product.class))).thenReturn(saved);

        ProductResponse result = productService.createProduct(request);

        assertEquals(10L, result.getId());
        assertEquals("Phone", result.getName());

        ArgumentCaptor<Product> captor = ArgumentCaptor.forClass(Product.class);
        verify(productRepository).save(captor.capture());
        assertEquals("Phone", captor.getValue().getName());
    }

    @Test
    void updateProduct_shouldUpdateAndReturnResponse() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(productRepository.save(any(Product.class))).thenAnswer(inv -> inv.getArgument(0));

        ProductResponse result = productService.updateProduct(1L, request);

        assertEquals("Phone", result.getName());
        assertEquals("Mobiles", result.getCategory());
        verify(productRepository).save(any(Product.class));
    }

    @Test
    void updateProduct_shouldThrowWhenNotFound() {
        when(productRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(ProductNotFoundException.class, () -> productService.updateProduct(1L, request));
    }

    @Test
    void updateQuantity_shouldUpdateQuantity() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(productRepository.save(any(Product.class))).thenAnswer(inv -> inv.getArgument(0));

        ProductResponse result = productService.updateQuantity(1L, 99);

        assertEquals(99, result.getQuantity());
        verify(productRepository).save(any(Product.class));
    }

    @Test
    void updateQuantity_shouldThrowWhenNotFound() {
        when(productRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(ProductNotFoundException.class, () -> productService.updateQuantity(1L, 99));
    }

//    @Test
//    void deleteProduct_shouldDeleteWhenExists() {
//        when(productRepository.existsById(1L)).thenReturn(true);
//
//        productService.deleteProduct(1L);
//
//        verify(productRepository).deleteById(1L);
//    }
//
//    @Test
//    void deleteProduct_shouldThrowWhenMissing() {
//        when(productRepository.existsById(1L)).thenReturn(false);
//
//        assertThrows(ProductNotFoundException.class, () -> productService.deleteProduct(1L));
//    }
}
