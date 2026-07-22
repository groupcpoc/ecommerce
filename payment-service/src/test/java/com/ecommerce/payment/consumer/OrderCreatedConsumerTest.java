package com.ecommerce.payment.consumer;

import com.ecommerce.events.OrderCreatedEvent;
import com.ecommerce.payment.service.PaymentService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderCreatedConsumerTest {

    @Mock
    private PaymentService paymentService;

    @InjectMocks
    private OrderCreatedConsumer orderCreatedConsumer;

    @Test
    void consume_callsProcessPayment() {
        OrderCreatedEvent event = OrderCreatedEvent.newBuilder()
                .setOrderId("order-id-001")
                .setUserId("user-id-001")
                .setAmount(499.99)
                .setItems(List.of("item-1"))
                .build();

        orderCreatedConsumer.consume(event);

        verify(paymentService).processPayment(event);
    }

    @Test
    void consume_serviceThrowsException_propagates() {
        OrderCreatedEvent event = OrderCreatedEvent.newBuilder()
                .setOrderId("order-id-001")
                .setUserId("user-id-001")
                .setAmount(499.99)
                .setItems(List.of("item-1"))
                .build();

        doThrow(new RuntimeException("Service error"))
                .when(paymentService).processPayment(event);

        org.junit.jupiter.api.Assertions.assertThrows(RuntimeException.class,
                () -> orderCreatedConsumer.consume(event));
    }
}