package com.ecommerce.product.consumer;

import com.ecommerce.product.model.Product;
import com.ecommerce.product.repository.ProductRepository;
import com.ecommerce.product.enums.ProductStatus;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class InventoryFailedConsumer {

    private final ProductRepository productRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @KafkaListener(
            topics = "inventory.failed",
            groupId = "product-service-group"
    )
    public void consumeInventoryFailed(String message) {
        log.info("Received inventory.failed event: {}", message);

        try {
            JsonNode jsonNode = objectMapper.readTree(message);
            String productId = jsonNode.get("productId").asText();
            String reason = jsonNode.get("reason") != null ? jsonNode.get("reason").asText() : "Unknown";

            log.info("Processing inventory failure for product: {}, reason: {}", productId, reason);

            Optional<Product> productOptional = productRepository.findBySku(productId);

            if (productOptional.isPresent()) {
                Product product = productOptional.get();

                if (product.getQuantity() <= 10) {
                    product.setStatus(ProductStatus.OUT_OF_STOCK);
                    log.info("Product {} marked as OUT_OF_STOCK", productId);
                } else {
                    product.setStatus(ProductStatus.LOW_STOCK);
                    log.info("Product {} marked as LOW_STOCK", productId);
                }

                productRepository.save(product);
                log.info("Product {} updated successfully", productId);
            } else {
                log.warn("Product with SKU {} not found", productId);
            }
        } catch (Exception e) {
            log.error("Error processing inventory.failed event: {}", e.getMessage(), e);
        }
    }
}