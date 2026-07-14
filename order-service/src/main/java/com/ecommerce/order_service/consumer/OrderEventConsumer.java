package com.ecommerce.order_service.consumer;

import com.ecommerce.order_service.entity.Order;
import com.ecommerce.order_service.enums.OrderStatus;
import com.ecommerce.order_service.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Consumes Kafka Avro events from Payment Service and Inventory Service
 * and updates order status accordingly.
 *
 * Topics consumed:
 *   - payment.failed    → sets order status to CANCELLED
 *   - inventory.failed  → sets order status to CANCELLED
 *   - inventory.reserved → sets order status to CONFIRMED
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OrderEventConsumer {

    private final OrderRepository orderRepository;

    // ─── payment.failed ─────────────────────────────────────────────────────────

    @KafkaListener(topics = "payment.failed", groupId = "order-service-group")
    @Transactional
    public void handlePaymentFailed(com.ecommerce.events.PaymentFailedEvent event) {
        log.info("Received [payment.failed] — {}", event);

        if (event.getOrderId() == null) return;
        String orderId = event.getOrderId().toString();

        updateOrderStatus(orderId, OrderStatus.CANCELLED, "payment.failed");
    }

    // ─── inventory.failed ───────────────────────────────────────────────────────

    @KafkaListener(topics = "inventory.failed", groupId = "order-service-group")
    @Transactional
    public void handleInventoryFailed(com.ecommerce.events.InventoryFailedEvent event) {
        log.info("Received [inventory.failed] — {}", event);

        if (event.getOrderId() == null) return;
        String orderId = event.getOrderId().toString();

        updateOrderStatus(orderId, OrderStatus.CANCELLED, "inventory.failed");
    }

    // ─── inventory.reserved ─────────────────────────────────────────────────────

    @KafkaListener(topics = "inventory.reserved", groupId = "order-service-group")
    @Transactional
    public void handleInventoryReserved(com.ecommerce.events.InventoryReservedEvent event) {
        log.info("Received [inventory.reserved] — {}", event);

        if (event.getOrderId() == null) return;
        String orderId = event.getOrderId().toString();

        updateOrderStatus(orderId, OrderStatus.CONFIRMED, "inventory.reserved");
    }

    // ─── helpers ────────────────────────────────────────────────────────────────

    /**
     * Looks up the order by its UUID-based orderId and transitions it to the
     * given target status. Skips if the order is already in a terminal state
     * (CANCELLED or DELIVERED) for idempotency.
     */
    private void updateOrderStatus(String orderId, OrderStatus targetStatus, String sourceTopic) {
        Optional<Order> optionalOrder = orderRepository.findByOrderId(orderId);

        if (optionalOrder.isEmpty()) {
            log.warn("[{}] Order not found for orderId={} — skipping", sourceTopic, orderId);
            return;
        }

        Order order = optionalOrder.get();

        // Idempotency guard: don't update orders already in a terminal state
        if (order.getStatus() == OrderStatus.CANCELLED || order.getStatus() == OrderStatus.DELIVERED) {
            log.warn("[{}] Order orderId={} is already {} — skipping status update to {}",
                    sourceTopic, orderId, order.getStatus(), targetStatus);
            return;
        }

        order.setStatus(targetStatus);
        order.setUpdatedAt(LocalDateTime.now());
        orderRepository.save(order);

        log.info("[{}] Order orderId={} status updated to {}", sourceTopic, orderId, targetStatus);
    }
}
