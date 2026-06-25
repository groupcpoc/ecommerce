package com.ecommerce.paymentservice.consumer;

import com.ecommerce.events.OrderCreatedEvent;
import com.ecommerce.paymentservice.service.PaymentService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class OrderCreatedConsumerTest {

    @Mock
    private PaymentService paymentService;

    @InjectMocks
    private OrderCreatedConsumer consumer;

    @Test
    void consume_delegatesToPaymentService() {
        OrderCreatedEvent event = mock(OrderCreatedEvent.class);

        consumer.consume(event);

        verify(paymentService).processPayment(event);
    }
}