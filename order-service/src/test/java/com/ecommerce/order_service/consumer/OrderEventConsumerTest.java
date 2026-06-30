package com.ecommerce.order_service.consumer;

import com.ecommerce.order_service.entity.Order;
import com.ecommerce.order_service.enums.OrderStatus;
import com.ecommerce.order_service.repository.OrderRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class OrderEventConsumerTest {

    @Mock
    private OrderRepository orderRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private OrderEventConsumer consumer;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        consumer = new OrderEventConsumer(orderRepository, objectMapper);
    }

    @Test
    void handlePaymentFailed_Success() {
        Order order = Order.builder().orderId("uuid-123").status(OrderStatus.PENDING).build();
        when(orderRepository.findByOrderId("uuid-123")).thenReturn(Optional.of(order));

        String message = "{\"orderId\":\"uuid-123\"}";
        consumer.handlePaymentFailed(message);

        assertEquals(OrderStatus.CANCELLED, order.getStatus());
        verify(orderRepository).save(order);
    }

    @Test
    void handlePaymentFailed_InvalidJson() {
        consumer.handlePaymentFailed("invalid-json");
        verify(orderRepository, never()).findByOrderId(anyString());
    }

    @Test
    void handleInventoryFailed_Success() {
        Order order = Order.builder().orderId("uuid-123").status(OrderStatus.PENDING).build();
        when(orderRepository.findByOrderId("uuid-123")).thenReturn(Optional.of(order));

        String message = "{\"orderId\":\"uuid-123\"}";
        consumer.handleInventoryFailed(message);

        assertEquals(OrderStatus.CANCELLED, order.getStatus());
        verify(orderRepository).save(order);
    }

    @Test
    void handleInventoryReserved_Success() {
        Order order = Order.builder().orderId("uuid-123").status(OrderStatus.PENDING).build();
        when(orderRepository.findByOrderId("uuid-123")).thenReturn(Optional.of(order));

        String message = "{\"orderId\":\"uuid-123\"}";
        consumer.handleInventoryReserved(message);

        assertEquals(OrderStatus.CONFIRMED, order.getStatus());
        verify(orderRepository).save(order);
    }

    @Test
    void updateOrderStatus_OrderNotFound() {
        when(orderRepository.findByOrderId("uuid-123")).thenReturn(Optional.empty());

        consumer.handleInventoryReserved("{\"orderId\":\"uuid-123\"}");

        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    void updateOrderStatus_TerminalState_Skips() {
        Order order = Order.builder().orderId("uuid-123").status(OrderStatus.CANCELLED).build();
        when(orderRepository.findByOrderId("uuid-123")).thenReturn(Optional.of(order));

        consumer.handleInventoryReserved("{\"orderId\":\"uuid-123\"}");

        verify(orderRepository, never()).save(any(Order.class));
    }
}
