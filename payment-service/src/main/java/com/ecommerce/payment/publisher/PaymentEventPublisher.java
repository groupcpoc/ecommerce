package com.ecommerce.payment.publisher;

import com.ecommerce.events.PaymentFailedEvent;
import com.ecommerce.events.PaymentProcessedEvent;
import com.ecommerce.events.RefundProcessedEvent;
import com.ecommerce.payment.entity.Payment;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentEventPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void publishPaymentProcessed(com.ecommerce.events.OrderCreatedEvent sourceEvent) {
        PaymentProcessedEvent event = PaymentProcessedEvent.newBuilder()
                .setOrderId(sourceEvent.getOrderId())
                .setUserId(sourceEvent.getUserId())
                .setAmount(sourceEvent.getAmount())
                .setStatus("SUCCESS")
                .build();
        kafkaTemplate.send("payment.processed", event);
        log.info("Published payment.processed for orderId: {}", sourceEvent.getOrderId());
    }

    public void publishPaymentFailed(com.ecommerce.events.OrderCreatedEvent sourceEvent, String reason) {
        PaymentFailedEvent event = PaymentFailedEvent.newBuilder()
                .setOrderId(sourceEvent.getOrderId())
                .setUserId(sourceEvent.getUserId())
                .setReason(reason)
                .build();
        kafkaTemplate.send("payment.failed", event);
        log.info("Published payment.failed for orderId: {}", sourceEvent.getOrderId());
    }

    public void publishRefundProcessed(Payment payment) {
        RefundProcessedEvent event = RefundProcessedEvent.newBuilder()
                .setOrderId(payment.getOrderId())
                .setUserId(payment.getUserId())
                .setAmount(payment.getAmount().doubleValue())
                .build();
        kafkaTemplate.send("refund.processed", event);
        log.info("Refund processed for paymentId: {}", payment.getId());
    }
}