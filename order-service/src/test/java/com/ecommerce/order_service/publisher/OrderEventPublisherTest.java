package com.ecommerce.order_service.publisher;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import java.util.concurrent.CompletableFuture;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OrderEventPublisherTest {

    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;

    private OrderEventPublisher publisher;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        publisher = new OrderEventPublisher(kafkaTemplate);
    }

    @Test
    @SuppressWarnings("unchecked")
    void publishOrderCreated_Success() {
        CompletableFuture<SendResult<String, String>> future = new CompletableFuture<>();
        when(kafkaTemplate.send(eq("order.created"), eq("uuid-123"), anyString())).thenReturn(future);

        publisher.publishOrderCreated("uuid-123", "user-1", "[\"Pizza\"]", 100.0);

        SendResult<String, String> sendResult = org.mockito.Mockito.mock(SendResult.class);
        org.apache.kafka.clients.producer.RecordMetadata metadata = 
            new org.apache.kafka.clients.producer.RecordMetadata(
                new org.apache.kafka.common.TopicPartition("order.created", 0),
                0, 0, 0, 0, 0
            );
        when(sendResult.getRecordMetadata()).thenReturn(metadata);
        future.complete(sendResult);

        verify(kafkaTemplate).send(eq("order.created"), eq("uuid-123"), anyString());
    }

    @Test
    void publishOrderCreated_Failure() {
        CompletableFuture<SendResult<String, String>> future = new CompletableFuture<>();
        when(kafkaTemplate.send(eq("order.created"), eq("uuid-123"), anyString())).thenReturn(future);

        publisher.publishOrderCreated("uuid-123", "user-1", "[\"Pizza\"]", 100.0);

        future.completeExceptionally(new RuntimeException("Kafka connection error"));

        verify(kafkaTemplate).send(eq("order.created"), eq("uuid-123"), anyString());
    }

    @Test
    @SuppressWarnings("unchecked")
    void publishOrderCancelled_Success() {
        CompletableFuture<SendResult<String, String>> future = new CompletableFuture<>();
        when(kafkaTemplate.send(eq("order.cancelled"), eq("uuid-123"), anyString())).thenReturn(future);

        publisher.publishOrderCancelled("uuid-123", "user-1", "CUSTOMER_CANCELLED");

        SendResult<String, String> sendResult = org.mockito.Mockito.mock(SendResult.class);
        org.apache.kafka.clients.producer.RecordMetadata metadata = 
            new org.apache.kafka.clients.producer.RecordMetadata(
                new org.apache.kafka.common.TopicPartition("order.cancelled", 0),
                0, 0, 0, 0, 0
            );
        when(sendResult.getRecordMetadata()).thenReturn(metadata);
        future.complete(sendResult);

        verify(kafkaTemplate).send(eq("order.cancelled"), eq("uuid-123"), anyString());
    }

    @Test
    void publishOrderCancelled_Failure() {
        CompletableFuture<SendResult<String, String>> future = new CompletableFuture<>();
        when(kafkaTemplate.send(eq("order.cancelled"), eq("uuid-123"), anyString())).thenReturn(future);

        publisher.publishOrderCancelled("uuid-123", "user-1", "CUSTOMER_CANCELLED");

        future.completeExceptionally(new RuntimeException("Kafka connection error"));

        verify(kafkaTemplate).send(eq("order.cancelled"), eq("uuid-123"), anyString());
    }
}
