package com.ecommerce.product.consumer;

import com.ecommerce.product.enums.ProductStatus;
import com.ecommerce.product.entity.Product;
import com.ecommerce.product.repository.ProductRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InventoryFailedConsumerTest {

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private InventoryFailedConsumer consumer;

    @Test
    void consumeInventoryFailed_shouldUpdateProduct_whenFound() {
        Product product = Product.builder()
                .id(1L)
                .name("Phone")
                .quantity(5)
                .sku("SKU-1")
                .status(ProductStatus.ACTIVE)
                .build();

        when(productRepository.findBySku("SKU-1")).thenReturn(Optional.of(product));
        when(productRepository.save(any(Product.class))).thenAnswer(inv -> inv.getArgument(0));

        consumer.consumeInventoryFailed("""
                {"productId":"SKU-1","reason":"inventory failed"}
                """);

        ArgumentCaptor<Product> captor = ArgumentCaptor.forClass(Product.class);
        verify(productRepository).save(captor.capture());

        Product saved = captor.getValue();
        assertEquals("SKU-1", saved.getSku());
        assertNotNull(saved.getStatus());
    }

    @Test
    void consumeInventoryFailed_shouldDoNothing_whenProductMissing() {
        when(productRepository.findBySku("SKU-3")).thenReturn(Optional.empty());

        consumer.consumeInventoryFailed("""
                {"productId":"SKU-3","reason":"missing"}
                """);

        verify(productRepository, never()).save(any());
    }

    @Test
    void consumeInventoryFailed_shouldHandleInvalidJson() {
        consumer.consumeInventoryFailed("invalid-json");
        verify(productRepository, never()).save(any());
    }

    @Test
    void consumeInventoryFailed_shouldMarkOutOfStock_whenQuantityLessThanOrEqual10() {
        Product product = Product.builder()
                .id(1L)
                .name("Phone")
                .quantity(10)
                .sku("SKU-LOW")
                .status(ProductStatus.ACTIVE)
                .build();

        when(productRepository.findBySku("SKU-LOW")).thenReturn(Optional.of(product));
        when(productRepository.save(any(Product.class))).thenAnswer(inv -> inv.getArgument(0));

        consumer.consumeInventoryFailed("""
                {"productId":"SKU-LOW","reason":"low inventory"}
                """);

        ArgumentCaptor<Product> captor = ArgumentCaptor.forClass(Product.class);
        verify(productRepository).save(captor.capture());

        Product saved = captor.getValue();
        assertEquals(ProductStatus.OUT_OF_STOCK, saved.getStatus());
    }

    @Test
    void consumeInventoryFailed_shouldMarkLowStock_whenQuantityGreaterThan10() {
        Product product = Product.builder()
                .id(1L)
                .name("Phone")
                .quantity(15)
                .sku("SKU-MID")
                .status(ProductStatus.ACTIVE)
                .build();

        when(productRepository.findBySku("SKU-MID")).thenReturn(Optional.of(product));
        when(productRepository.save(any(Product.class))).thenAnswer(inv -> inv.getArgument(0));

        consumer.consumeInventoryFailed("""
                {"productId":"SKU-MID","reason":"inventory issue"}
                """);

        ArgumentCaptor<Product> captor = ArgumentCaptor.forClass(Product.class);
        verify(productRepository).save(captor.capture());

        Product saved = captor.getValue();
        assertEquals(ProductStatus.LOW_STOCK, saved.getStatus());
    }

    @Test
    void consumeInventoryFailed_shouldMarkOutOfStock_whenQuantityZero() {
        Product product = Product.builder()
                .id(1L)
                .name("Phone")
                .quantity(0)
                .sku("SKU-ZERO")
                .status(ProductStatus.ACTIVE)
                .build();

        when(productRepository.findBySku("SKU-ZERO")).thenReturn(Optional.of(product));
        when(productRepository.save(any(Product.class))).thenAnswer(inv -> inv.getArgument(0));

        consumer.consumeInventoryFailed("""
                {"productId":"SKU-ZERO","reason":"out of stock"}
                """);

        ArgumentCaptor<Product> captor = ArgumentCaptor.forClass(Product.class);
        verify(productRepository).save(captor.capture());

        Product saved = captor.getValue();
        assertEquals(ProductStatus.OUT_OF_STOCK, saved.getStatus());
    }

    @Test
    void consumeInventoryFailed_shouldHandleNullReason() {
        Product product = Product.builder()
                .id(1L)
                .name("Phone")
                .quantity(5)
                .sku("SKU-NULLREASON")
                .status(ProductStatus.ACTIVE)
                .build();

        when(productRepository.findBySku("SKU-NULLREASON")).thenReturn(Optional.of(product));
        when(productRepository.save(any(Product.class))).thenAnswer(inv -> inv.getArgument(0));

        consumer.consumeInventoryFailed("""
                {"productId":"SKU-NULLREASON"}
                """);

        ArgumentCaptor<Product> captor = ArgumentCaptor.forClass(Product.class);
        verify(productRepository).save(captor.capture());

        Product saved = captor.getValue();
        assertEquals("SKU-NULLREASON", saved.getSku());
    }

    @Test
    void consumeInventoryFailed_shouldHandleEmptyJson() {
        consumer.consumeInventoryFailed("{}");
        verify(productRepository, never()).save(any());
    }

    @Test
    void consumeInventoryFailed_shouldHandleMalformedJson() {
        consumer.consumeInventoryFailed("{productId: SKU-1}");
        verify(productRepository, never()).save(any());
    }

    @Test
    void consumeInventoryFailed_shouldHandleEmptyString() {
        consumer.consumeInventoryFailed("");
        verify(productRepository, never()).save(any());
    }

    @Test
    void consumeInventoryFailed_shouldHandleNullProductId() {
        consumer.consumeInventoryFailed("""
                {"reason":"test"}
                """);
        verify(productRepository, never()).save(any());
    }
}