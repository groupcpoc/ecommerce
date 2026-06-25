package com.ecommerce.paymentservice.consumer;

import com.ecommerce.events.OrderCancelledEvent;
import com.ecommerce.paymentservice.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderCancelledConsumer {

    private final PaymentService paymentService;

    @KafkaListener(topics = "order.cancelled", groupId = "payment-service-group")
    public void consume(OrderCancelledEvent event) {
        log.info("Received order.cancelled event for orderId: {}", event.getOrderId());
        paymentService.handleOrderCancelled(event);
    }
}