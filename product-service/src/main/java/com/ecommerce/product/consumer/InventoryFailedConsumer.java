package com.ecommerce.product.consumer;

import com.ecommerce.events.InventoryFailedEvent;
import com.ecommerce.product.entity.Product;
import com.ecommerce.product.enums.ProductStatus;
import com.ecommerce.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class InventoryFailedConsumer {

    private final ProductRepository productRepository;

    @KafkaListener(
            topics = "inventory.failed",
            groupId = "product-service-group",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consume(InventoryFailedEvent event) {

        log.info("Inventory Failed Event Received : {}", event);

        String productId = event.getProductId().toString();
        String reason = event.getReason().toString();

        Product product = productRepository.findBySku(productId)
                .orElse(null);

        if (product == null) {
            log.warn("Product not found for sku {}", productId);
            return;
        }

        if (product.getQuantity() == 0) {
            product.setStatus(ProductStatus.OUT_OF_STOCK);
        } else if (product.getQuantity() <= 10) {
            product.setStatus(ProductStatus.LOW_STOCK);
        }

        productRepository.save(product);

        log.info("Product updated after inventory failure. SKU={} Reason={}",
                productId,
                reason);
    }
}