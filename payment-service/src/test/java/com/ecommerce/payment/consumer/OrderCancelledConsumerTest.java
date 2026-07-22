package com.ecommerce.payment.consumer;

import com.ecommerce.events.OrderCancelledEvent;
import com.ecommerce.payment.service.PaymentService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderCancelledConsumerTest {

    @Mock
    private PaymentService paymentService;

    @InjectMocks
    private OrderCancelledConsumer orderCancelledConsumer;

    @Test
    void consume_callsHandleOrderCancelled() {
        OrderCancelledEvent event = OrderCancelledEvent.newBuilder()
                .setOrderId("order-id-001")
                .setUserId("user-id-001")
                .setReason("Customer cancelled")
                .build();

        orderCancelledConsumer.consume(event);

        verify(paymentService).handleOrderCancelled(event);
    }

    @Test
    void consume_serviceThrowsException_propagates() {
        OrderCancelledEvent event = OrderCancelledEvent.newBuilder()
                .setOrderId("order-id-001")
                .setUserId("user-id-001")
                .setReason("Customer cancelled")
                .build();

        doThrow(new RuntimeException("Service error"))
                .when(paymentService).handleOrderCancelled(event);

        org.junit.jupiter.api.Assertions.assertThrows(RuntimeException.class,
                () -> orderCancelledConsumer.consume(event));
    }
}