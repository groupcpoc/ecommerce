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
    void publishOrderCreated_Success() {
        CompletableFuture<SendResult<String, String>> future = new CompletableFuture<>();
        when(kafkaTemplate.send(eq("order.created"), eq("uuid-123"), anyString())).thenReturn(future);

        publisher.publishOrderCreated("uuid-123", "user-1", "[\"Pizza\"]", 100.0);

        verify(kafkaTemplate).send(eq("order.created"), eq("uuid-123"), anyString());
    }

    @Test
    void publishOrderCancelled_Success() {
        CompletableFuture<SendResult<String, String>> future = new CompletableFuture<>();
        when(kafkaTemplate.send(eq("order.cancelled"), eq("uuid-123"), anyString())).thenReturn(future);

        publisher.publishOrderCancelled("uuid-123", "user-1", "CUSTOMER_CANCELLED");

        verify(kafkaTemplate).send(eq("order.cancelled"), eq("uuid-123"), anyString());
    }
}
