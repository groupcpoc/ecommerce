package com.ecommerce.order_service.consumer;

import com.ecommerce.order_service.entity.Order;
import com.ecommerce.order_service.enums.OrderStatus;
import com.ecommerce.order_service.repository.OrderRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Consumes Kafka events from Payment Service and Inventory Service
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
    private final ObjectMapper objectMapper;

    // ─── payment.failed ─────────────────────────────────────────────────────────

    @KafkaListener(topics = "payment.failed", groupId = "order-service-group")
    @Transactional
    public void handlePaymentFailed(String message) {
        log.info("Received [payment.failed] — {}", message);

        String orderId = extractOrderId(message);
        if (orderId == null) return;

        updateOrderStatus(orderId, OrderStatus.CANCELLED, "payment.failed");
    }

    // ─── inventory.failed ───────────────────────────────────────────────────────

    @KafkaListener(topics = "inventory.failed", groupId = "order-service-group")
    @Transactional
    public void handleInventoryFailed(String message) {
        log.info("Received [inventory.failed] — {}", message);

        String orderId = extractOrderId(message);
        if (orderId == null) return;

        updateOrderStatus(orderId, OrderStatus.CANCELLED, "inventory.failed");
    }

    // ─── inventory.reserved ─────────────────────────────────────────────────────

    @KafkaListener(topics = "inventory.reserved", groupId = "order-service-group")
    @Transactional
    public void handleInventoryReserved(String message) {
        log.info("Received [inventory.reserved] — {}", message);

        String orderId = extractOrderId(message);
        if (orderId == null) return;

        updateOrderStatus(orderId, OrderStatus.CONFIRMED, "inventory.reserved");
    }

    // ─── helpers ────────────────────────────────────────────────────────────────

    /**
     * Extracts the "orderId" field from a JSON message payload.
     */
    private String extractOrderId(String message) {
        try {
            JsonNode node = objectMapper.readTree(message);
            String orderId = node.get("orderId").asText();
            return orderId;
        } catch (Exception e) {
            log.error("Failed to parse orderId from Kafka message: {} — {}", message, e.getMessage());
            return null;
        }
    }

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
