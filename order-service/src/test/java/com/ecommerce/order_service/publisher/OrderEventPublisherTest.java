package com.ecommerce.order_service.publisher;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OrderEventPublisherTest {

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    private OrderEventPublisher publisher;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        publisher = new OrderEventPublisher(kafkaTemplate);
    }

    @Test
    @SuppressWarnings("unchecked")
    void publishOrderCreated_Success() {
        CompletableFuture<SendResult<String, Object>> future = new CompletableFuture<>();
        when(kafkaTemplate.send(eq("order.created"), eq("uuid-123"), any())).thenReturn(future);

        publisher.publishOrderCreated("uuid-123", "user-1", List.of("Pizza"), 100.0);

        SendResult<String, Object> sendResult = org.mockito.Mockito.mock(SendResult.class);
        org.apache.kafka.clients.producer.RecordMetadata metadata = 
            new org.apache.kafka.clients.producer.RecordMetadata(
                new org.apache.kafka.common.TopicPartition("order.created", 0),
                0, 0, 0, 0, 0
            );
        when(sendResult.getRecordMetadata()).thenReturn(metadata);
        future.complete(sendResult);

        verify(kafkaTemplate).send(eq("order.created"), eq("uuid-123"), any());
    }

    @Test
    void publishOrderCreated_Failure() {
        CompletableFuture<SendResult<String, Object>> future = new CompletableFuture<>();
        when(kafkaTemplate.send(eq("order.created"), eq("uuid-123"), any())).thenReturn(future);

        publisher.publishOrderCreated("uuid-123", "user-1", List.of("Pizza"), 100.0);

        future.completeExceptionally(new RuntimeException("Kafka connection error"));

        verify(kafkaTemplate).send(eq("order.created"), eq("uuid-123"), any());
    }

    @Test
    @SuppressWarnings("unchecked")
    void publishOrderCancelled_Success() {
        CompletableFuture<SendResult<String, Object>> future = new CompletableFuture<>();
        when(kafkaTemplate.send(eq("order.cancelled"), eq("uuid-123"), any())).thenReturn(future);

        publisher.publishOrderCancelled("uuid-123", "user-1", "CUSTOMER_CANCELLED");

        SendResult<String, Object> sendResult = org.mockito.Mockito.mock(SendResult.class);
        org.apache.kafka.clients.producer.RecordMetadata metadata = 
            new org.apache.kafka.clients.producer.RecordMetadata(
                new org.apache.kafka.common.TopicPartition("order.cancelled", 0),
                0, 0, 0, 0, 0
            );
        when(sendResult.getRecordMetadata()).thenReturn(metadata);
        future.complete(sendResult);

        verify(kafkaTemplate).send(eq("order.cancelled"), eq("uuid-123"), any());
    }

    @Test
    void publishOrderCancelled_Failure() {
        CompletableFuture<SendResult<String, Object>> future = new CompletableFuture<>();
        when(kafkaTemplate.send(eq("order.cancelled"), eq("uuid-123"), any())).thenReturn(future);

        publisher.publishOrderCancelled("uuid-123", "user-1", "CUSTOMER_CANCELLED");

        future.completeExceptionally(new RuntimeException("Kafka connection error"));

        verify(kafkaTemplate).send(eq("order.cancelled"), eq("uuid-123"), any());
    }
}
