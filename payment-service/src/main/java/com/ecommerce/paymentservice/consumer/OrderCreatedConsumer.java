package com.ecommerce.paymentservice.consumer;

import com.ecommerce.events.OrderCreatedEvent;
import com.ecommerce.paymentservice.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderCreatedConsumer {

    private final PaymentService paymentService;

    @KafkaListener(topics = "order.created", groupId = "payment-service-group")
    public void consume(OrderCreatedEvent event) {
        log.info("Received order.created event for orderId: {}", event.getOrderId());
        paymentService.processPayment(event);
    }
}