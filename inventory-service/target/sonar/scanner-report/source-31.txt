package com.ecommerce.inventoryservice.publisher;

import com.ecommerce.inventoryservice.event.InventoryEvent;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(name = "app.kafka.enabled", havingValue = "false", matchIfMissing = true)
public class NoOpKafkaEventPublisher implements KafkaEventPublisher {

    @Override
    public void publish(String topic, InventoryEvent event) {
        // Kafka is disabled for this profile.
    }
}
