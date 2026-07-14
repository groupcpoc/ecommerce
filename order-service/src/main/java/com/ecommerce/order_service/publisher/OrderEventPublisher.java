package com.ecommerce.order_service.publisher;

import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Component
@Slf4j
@SuppressWarnings("null")
public class OrderEventPublisher {

    private static final String ORDER_CREATED_TOPIC = "order.created";
    private static final String ORDER_CANCELLED_TOPIC = "order.cancelled";

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public OrderEventPublisher(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publishOrderCreated(String orderId, String userId, List<String> items, Double amount) {
        List<CharSequence> avroItems = items.stream()
                .map(item -> (CharSequence) item)
                .collect(Collectors.toList());

        com.ecommerce.events.OrderCreatedEvent event = com.ecommerce.events.OrderCreatedEvent.newBuilder()
                .setOrderId(orderId)
                .setUserId(userId)
                .setAmount(amount)
                .setItems(avroItems)
                .build();

        log.info("Publishing to Kafka topic [{}] — payload: {}", ORDER_CREATED_TOPIC, event);

        CompletableFuture<SendResult<String, Object>> future = kafkaTemplate.send(ORDER_CREATED_TOPIC, orderId, event);

        future.whenComplete((result, ex) -> {
            if (ex != null) {
                log.error("Failed to publish order.created for orderId={} — {}", orderId, ex.getMessage());
            } else {
                log.info("order.created published — orderId={}, partition={}, offset={}",
                        orderId,
                        result.getRecordMetadata().partition(),
                        result.getRecordMetadata().offset());
            }
        });
    }

    public void publishOrderCancelled(String orderId, String userId, String reason) {
        com.ecommerce.events.OrderCancelledEvent event = com.ecommerce.events.OrderCancelledEvent.newBuilder()
                .setOrderId(orderId)
                .setUserId(userId)
                .setReason(reason)
                .build();

        log.info("Publishing to Kafka topic [{}] — payload: {}", ORDER_CANCELLED_TOPIC, event);

        CompletableFuture<SendResult<String, Object>> future = kafkaTemplate.send(ORDER_CANCELLED_TOPIC, orderId, event);

        future.whenComplete((result, ex) -> {
            if (ex != null) {
                log.error("Failed to publish order.cancelled for orderId={} — {}", orderId, ex.getMessage());
            } else {
                log.info("order.cancelled published — orderId={}, partition={}, offset={}",
                        orderId,
                        result.getRecordMetadata().partition(),
                        result.getRecordMetadata().offset());
            }
        });
    }
}
