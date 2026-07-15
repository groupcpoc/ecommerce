package com.ecommerce.payment.publisher;

import com.ecommerce.events.OrderCreatedEvent;
import com.ecommerce.payment.entity.Payment;
import com.ecommerce.payment.entity.PaymentStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentEventPublisherTest {

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    @InjectMocks
    private PaymentEventPublisher paymentEventPublisher;

    private OrderCreatedEvent buildOrderCreatedEvent() {
        return OrderCreatedEvent.newBuilder()
                .setOrderId("order-id-001")
                .setUserId("user-id-001")
                .setAmount(499.99)
                .setItems(List.of("item-1"))
                .build();
    }

    private Payment buildPayment() {
        return Payment.builder()
                .id("payment-id-001")
                .orderId("order-id-001")
                .userId("user-id-001")
                .amount(BigDecimal.valueOf(499.99))
                .status(PaymentStatus.REFUNDED)
                .razorpayId("order_test123")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    // ─── publishPaymentProcessed ──────────────────────────────────────────────

    @Test
    void publishPaymentProcessed_sendsToCorrectTopic() {
        OrderCreatedEvent event = buildOrderCreatedEvent();

        paymentEventPublisher.publishPaymentProcessed(event);

        verify(kafkaTemplate).send(eq("payment.processed"), any());
    }

    @Test
    void publishPaymentProcessed_sendsOnce() {
        OrderCreatedEvent event = buildOrderCreatedEvent();

        paymentEventPublisher.publishPaymentProcessed(event);

        verify(kafkaTemplate, times(1)).send(eq("payment.processed"), any());
    }

    // ─── publishPaymentFailed ─────────────────────────────────────────────────

    @Test
    void publishPaymentFailed_sendsToCorrectTopic() {
        OrderCreatedEvent event = buildOrderCreatedEvent();

        paymentEventPublisher.publishPaymentFailed(event, "Authentication failed");

        verify(kafkaTemplate).send(eq("payment.failed"), any());
    }

    @Test
    void publishPaymentFailed_sendsOnce() {
        OrderCreatedEvent event = buildOrderCreatedEvent();

        paymentEventPublisher.publishPaymentFailed(event, "Authentication failed");

        verify(kafkaTemplate, times(1)).send(eq("payment.failed"), any());
    }

    // ─── publishRefundProcessed ───────────────────────────────────────────────

    @Test
    void publishRefundProcessed_sendsToCorrectTopic() {
        Payment payment = buildPayment();

        paymentEventPublisher.publishRefundProcessed(payment);

        verify(kafkaTemplate).send(eq("refund.processed"), any());
    }

    @Test
    void publishRefundProcessed_sendsOnce() {
        Payment payment = buildPayment();

        paymentEventPublisher.publishRefundProcessed(payment);

        verify(kafkaTemplate, times(1)).send(eq("refund.processed"), any());
    }
}