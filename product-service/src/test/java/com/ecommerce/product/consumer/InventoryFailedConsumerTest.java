package com.ecommerce.product.consumer;

import com.ecommerce.events.InventoryFailedEvent;
import com.ecommerce.product.entity.Product;
import com.ecommerce.product.enums.ProductStatus;
import com.ecommerce.product.repository.ProductRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InventoryFailedConsumerTest {

    @Mock
    private ProductRepository repository;

    @InjectMocks
    private InventoryFailedConsumer consumer;

    private InventoryFailedEvent event(String sku, String reason) {

        InventoryFailedEvent event = new InventoryFailedEvent();

        event.setOrderId("ORD-100");
        event.setProductId(sku);
        event.setReason(reason);

        return event;
    }

    @Test
    void shouldUpdateToOutOfStockWhenQuantityZero() {

        Product product = Product.builder()
                .sku("SKU1")
                .quantity(0)
                .status(ProductStatus.ACTIVE)
                .build();

        when(repository.findBySku("SKU1"))
                .thenReturn(Optional.of(product));

        consumer.consume(event("SKU1","Out of stock"));

        verify(repository).save(product);

        assertEquals(ProductStatus.OUT_OF_STOCK,
                product.getStatus());
    }

    @Test
    void shouldUpdateToLowStockWhenQuantityLessThanTen() {

        Product product = Product.builder()
                .sku("SKU2")
                .quantity(5)
                .status(ProductStatus.ACTIVE)
                .build();

        when(repository.findBySku("SKU2"))
                .thenReturn(Optional.of(product));

        consumer.consume(event("SKU2","Low stock"));

        verify(repository).save(product);

        assertEquals(ProductStatus.LOW_STOCK,
                product.getStatus());
    }

    @Test
    void shouldNotChangeStatusWhenQuantityGreaterThanTen() {

        Product product = Product.builder()
                .sku("SKU3")
                .quantity(50)
                .status(ProductStatus.ACTIVE)
                .build();

        when(repository.findBySku("SKU3"))
                .thenReturn(Optional.of(product));

        consumer.consume(event("SKU3","Inventory failed"));

        verify(repository).save(product);

        assertEquals(ProductStatus.ACTIVE,
                product.getStatus());
    }

    @Test
    void shouldNotSaveWhenProductNotFound() {

        when(repository.findBySku("SKU4"))
                .thenReturn(Optional.empty());

        consumer.consume(event("SKU4","Not found"));

        verify(repository, never()).save(any());
    }

    @Test
    void shouldSaveUpdatedProduct() {

        Product product = Product.builder()
                .sku("SKU5")
                .quantity(2)
                .status(ProductStatus.ACTIVE)
                .build();

        when(repository.findBySku("SKU5"))
                .thenReturn(Optional.of(product));

        consumer.consume(event("SKU5","Inventory failed"));

        ArgumentCaptor<Product> captor =
                ArgumentCaptor.forClass(Product.class);

        verify(repository).save(captor.capture());

        assertEquals("SKU5",
                captor.getValue().getSku());
    }

    @Test
    void shouldCallRepositoryOnce() {

        Product product = Product.builder()
                .sku("SKU6")
                .quantity(0)
                .status(ProductStatus.ACTIVE)
                .build();

        when(repository.findBySku("SKU6"))
                .thenReturn(Optional.of(product));

        consumer.consume(event("SKU6","Reason"));

        verify(repository,times(1))
                .findBySku("SKU6");

        verify(repository,times(1))
                .save(product);
    }
}