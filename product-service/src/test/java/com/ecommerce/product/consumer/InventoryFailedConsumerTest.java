package com.ecommerce.product.consumer;

import com.ecommerce.events.InventoryFailedEvent;
import com.ecommerce.product.entity.Product;
import com.ecommerce.product.enums.ProductStatus;
import com.ecommerce.product.repository.ProductRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InventoryFailedConsumerTest {

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private InventoryFailedConsumer consumer;

    @Test
    void consume_shouldSetOutOfStockWhenQuantityZero() {
        InventoryFailedEvent event = mock(InventoryFailedEvent.class);
        when(event.getProductId()).thenReturn("1");
        when(event.getReason()).thenReturn("failed");

        Product product = Product.builder()
                .id(1L)
                .sku("1")
                .quantity(0)
                .status(ProductStatus.ACTIVE)
                .build();

        when(productRepository.findBySku("1")).thenReturn(Optional.of(product));

        consumer.consume(event);

        verify(productRepository).save(product);
        assert product.getStatus() == ProductStatus.OUT_OF_STOCK;
    }

    @Test
    void consume_shouldSetLowStockWhenQuantityLessOrEqual10() {
        InventoryFailedEvent event = mock(InventoryFailedEvent.class);
        when(event.getProductId()).thenReturn("2");
        when(event.getReason()).thenReturn("failed");

        Product product = Product.builder()
                .id(2L)
                .sku("2")
                .quantity(5)
                .status(ProductStatus.ACTIVE)
                .build();

        when(productRepository.findBySku("2")).thenReturn(Optional.of(product));

        consumer.consume(event);

        verify(productRepository).save(product);
        assert product.getStatus() == ProductStatus.LOW_STOCK;
    }

    @Test
    void consume_shouldKeepStatusWhenQuantityGreaterThan10() {
        InventoryFailedEvent event = mock(InventoryFailedEvent.class);
        when(event.getProductId()).thenReturn("3");
        when(event.getReason()).thenReturn("failed");

        Product product = Product.builder()
                .id(3L)
                .sku("3")
                .quantity(20)
                .status(ProductStatus.ACTIVE)
                .build();

        when(productRepository.findBySku("3")).thenReturn(Optional.of(product));

        consumer.consume(event);

        verify(productRepository).save(product);
        assert product.getStatus() == ProductStatus.ACTIVE;
    }

    @Test
    void consume_shouldReturnWhenProductNotFound() {
        InventoryFailedEvent event = mock(InventoryFailedEvent.class);
        when(event.getProductId()).thenReturn("4");
        when(event.getReason()).thenReturn("failed");

        when(productRepository.findBySku("4")).thenReturn(Optional.empty());

        consumer.consume(event);

        verify(productRepository, never()).save(any());
    }
}