package com.ecommerce.paymentservice.publisher;

import com.ecommerce.events.OrderCreatedEvent;
import com.ecommerce.paymentservice.entity.Payment;
import com.ecommerce.paymentservice.entity.PaymentStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentEventPublisherTest {

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    @InjectMocks
    private PaymentEventPublisher publisher;

    @Test
    void publishPaymentProcessed_sendsToCorrectTopic() {
        OrderCreatedEvent sourceEvent = mock(OrderCreatedEvent.class);
        when(sourceEvent.getOrderId()).thenReturn("order-1");
        when(sourceEvent.getUserId()).thenReturn("user-1");
        when(sourceEvent.getAmount()).thenReturn(100.0);

        publisher.publishPaymentProcessed(sourceEvent);

        verify(kafkaTemplate).send(eq("payment.processed"), any());
    }

    @Test
    void publishPaymentFailed_sendsToCorrectTopic() {
        OrderCreatedEvent sourceEvent = mock(OrderCreatedEvent.class);
        when(sourceEvent.getOrderId()).thenReturn("order-2");
        when(sourceEvent.getUserId()).thenReturn("user-2");

        publisher.publishPaymentFailed(sourceEvent, "Gateway timeout");

        verify(kafkaTemplate).send(eq("payment.failed"), any());
    }

    @Test
    void publishRefundProcessed_sendsToCorrectTopic() {
        Payment payment = Payment.builder()
                .id("p1")
                .orderId("order-1")
                .userId("user-1")
                .amount(100.0)
                .status(PaymentStatus.REFUNDED)
                .build();

        publisher.publishRefundProcessed(payment);

        verify(kafkaTemplate).send(eq("refund.processed"), any());
    }
}