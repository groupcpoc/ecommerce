package com.ecommerce.inventoryservice.publisher;

import com.ecommerce.inventoryservice.event.InventoryEvent;

public interface KafkaEventPublisher {
    void publish(String topic, InventoryEvent event);
}
