package com.ecommerce.inventoryservice.service;

import com.ecommerce.inventoryservice.event.InventoryEvent;

public interface KafkaEventPublisher {

    void publish(String topic, InventoryEvent event);
}
