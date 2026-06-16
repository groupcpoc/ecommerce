package com.ecommerce.inventoryservice.service.impl;

import com.ecommerce.inventoryservice.event.PaymentProcessedEvent;
import com.ecommerce.inventoryservice.event.OrderCancelledEvent;
import com.ecommerce.inventoryservice.service.InventoryService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "app.kafka.enabled", havingValue = "true")
public class KafkaInventoryListener {

    private final InventoryService inventoryService;
    private final ObjectMapper objectMapper;

    public KafkaInventoryListener(InventoryService inventoryService, ObjectMapper objectMapper) {
        this.inventoryService = inventoryService;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = "${app.kafka.topics.payment-processed}", groupId = "${spring.kafka.consumer.group-id}")
    public void consumePaymentProcessed(String payload) throws Exception {
        PaymentProcessedEvent event = objectMapper.readValue(payload, PaymentProcessedEvent.class);
        inventoryService.processPaymentProcessedEvent(event);
    }

    @KafkaListener(topics = "${app.kafka.topics.order-cancelled}", groupId = "${spring.kafka.consumer.group-id}")
    public void consumeOrderCancelled(String payload) throws Exception {
        OrderCancelledEvent event = objectMapper.readValue(payload, OrderCancelledEvent.class);
        inventoryService.releaseInventoryForOrder(event);
    }
}
