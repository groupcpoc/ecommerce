package com.ecommerce.paymentservice.consumer;

import com.ecommerce.events.OrderCancelledEvent;
import com.ecommerce.paymentservice.service.PaymentService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class OrderCancelledConsumerTest {

    @Mock
    private PaymentService paymentService;

    @InjectMocks
    private OrderCancelledConsumer consumer;

    @Test
    void consume_delegatesToPaymentService() {
        OrderCancelledEvent event = mock(OrderCancelledEvent.class);

        consumer.consume(event);

        verify(paymentService).handleOrderCancelled(event);
    }
}