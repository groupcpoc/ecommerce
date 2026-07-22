package com.ecommerce.order_service.consumer;

import com.ecommerce.order_service.entity.Order;
import com.ecommerce.order_service.enums.OrderStatus;
import com.ecommerce.order_service.repository.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class OrderEventConsumerTest {

    @Mock
    private OrderRepository orderRepository;

    private OrderEventConsumer consumer;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        consumer = new OrderEventConsumer(orderRepository);
    }

    @Test
    void handlePaymentFailed_Success() {
        Order order = Order.builder().orderId("uuid-123").status(OrderStatus.PENDING).build();
        when(orderRepository.findByOrderId("uuid-123")).thenReturn(Optional.of(order));

        com.ecommerce.events.PaymentFailedEvent event = com.ecommerce.events.PaymentFailedEvent.newBuilder()
                .setOrderId("uuid-123")
                .setUserId("user-123")
                .setReason("Card declined")
                .build();

        consumer.handlePaymentFailed(event);

        assertEquals(OrderStatus.CANCELLED, order.getStatus());
        verify(orderRepository).save(order);
    }

    @Test
    void handleInventoryFailed_Success() {
        Order order = Order.builder().orderId("uuid-123").status(OrderStatus.PENDING).build();
        when(orderRepository.findByOrderId("uuid-123")).thenReturn(Optional.of(order));

        com.ecommerce.events.InventoryFailedEvent event = com.ecommerce.events.InventoryFailedEvent.newBuilder()
                .setOrderId("uuid-123")
                .setProductId("prod-456")
                .setReason("Out of stock")
                .build();

        consumer.handleInventoryFailed(event);

        assertEquals(OrderStatus.CANCELLED, order.getStatus());
        verify(orderRepository).save(order);
    }

    @Test
    void handleInventoryReserved_Success() {
        Order order = Order.builder().orderId("uuid-123").status(OrderStatus.PENDING).build();
        when(orderRepository.findByOrderId("uuid-123")).thenReturn(Optional.of(order));

        com.ecommerce.events.InventoryReservedEvent event = com.ecommerce.events.InventoryReservedEvent.newBuilder()
                .setOrderId("uuid-123")
                .setProductId("prod-456")
                .setQuantity(2)
                .build();

        consumer.handleInventoryReserved(event);

        assertEquals(OrderStatus.CONFIRMED, order.getStatus());
        verify(orderRepository).save(order);
    }

    @Test
    void updateOrderStatus_OrderNotFound() {
        when(orderRepository.findByOrderId("uuid-123")).thenReturn(Optional.empty());

        com.ecommerce.events.InventoryReservedEvent event = com.ecommerce.events.InventoryReservedEvent.newBuilder()
                .setOrderId("uuid-123")
                .setProductId("prod-456")
                .setQuantity(2)
                .build();

        consumer.handleInventoryReserved(event);

        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    void updateOrderStatus_TerminalState_Skips() {
        Order order = Order.builder().orderId("uuid-123").status(OrderStatus.CANCELLED).build();
        when(orderRepository.findByOrderId("uuid-123")).thenReturn(Optional.of(order));

        com.ecommerce.events.InventoryReservedEvent event = com.ecommerce.events.InventoryReservedEvent.newBuilder()
                .setOrderId("uuid-123")
                .setProductId("prod-456")
                .setQuantity(2)
                .build();

        consumer.handleInventoryReserved(event);

        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    void updateOrderStatus_TerminalState_Delivered_Skips() {
        Order order = Order.builder().orderId("uuid-123").status(OrderStatus.DELIVERED).build();
        when(orderRepository.findByOrderId("uuid-123")).thenReturn(Optional.of(order));

        com.ecommerce.events.InventoryReservedEvent event = com.ecommerce.events.InventoryReservedEvent.newBuilder()
                .setOrderId("uuid-123")
                .setProductId("prod-456")
                .setQuantity(2)
                .build();

        consumer.handleInventoryReserved(event);

        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    void handlePaymentFailed_NullOrderId_ReturnsImmediately() {
        com.ecommerce.events.PaymentFailedEvent event = mock(com.ecommerce.events.PaymentFailedEvent.class);
        when(event.getOrderId()).thenReturn(null);

        consumer.handlePaymentFailed(event);

        verifyNoInteractions(orderRepository);
    }

    @Test
    void handleInventoryFailed_NullOrderId_ReturnsImmediately() {
        com.ecommerce.events.InventoryFailedEvent event = mock(com.ecommerce.events.InventoryFailedEvent.class);
        when(event.getOrderId()).thenReturn(null);

        consumer.handleInventoryFailed(event);

        verifyNoInteractions(orderRepository);
    }

    @Test
    void handleInventoryReserved_NullOrderId_ReturnsImmediately() {
        com.ecommerce.events.InventoryReservedEvent event = mock(com.ecommerce.events.InventoryReservedEvent.class);
        when(event.getOrderId()).thenReturn(null);

        consumer.handleInventoryReserved(event);

        verifyNoInteractions(orderRepository);
    }
}


