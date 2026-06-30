package com.ecommerce.order_service.publisher;

import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

@Component
@Slf4j
@SuppressWarnings("null")
public class OrderEventPublisher {

    private static final String ORDER_CREATED_TOPIC = "order.created";
    private static final String ORDER_CANCELLED_TOPIC = "order.cancelled";

    private final KafkaTemplate<String, String> kafkaTemplate;

    public OrderEventPublisher(KafkaTemplate<String, String> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publishOrderCreated(String orderId, String userId, String items, Double amount) {
        String payload = String.format(
                "{\"orderId\":\"%s\",\"userId\":\"%s\",\"items\":%s,\"amount\":%.2f}",
                orderId, userId, items, amount);

        log.info("Publishing to Kafka topic [{}] — payload: {}", ORDER_CREATED_TOPIC, payload);

        CompletableFuture<SendResult<String, String>> future = kafkaTemplate.send(ORDER_CREATED_TOPIC, orderId,
                payload);

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
        String payload = String.format(
                "{\"orderId\":\"%s\",\"userId\":\"%s\",\"reason\":\"%s\"}",
                orderId, userId, reason);

        log.info("Publishing to Kafka topic [{}] — payload: {}", ORDER_CANCELLED_TOPIC, payload);

        CompletableFuture<SendResult<String, String>> future = kafkaTemplate.send(ORDER_CANCELLED_TOPIC, orderId,
                payload);

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
