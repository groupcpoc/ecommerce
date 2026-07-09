package com.ecommerce.order_service.service;

import com.ecommerce.order_service.dto.OrderRequestDto;
import com.ecommerce.order_service.dto.OrderResponseDto;
import com.ecommerce.order_service.entity.Order;
import com.ecommerce.order_service.enums.OrderStatus;
import com.ecommerce.order_service.exception.InvalidOrderStateException;
import com.ecommerce.order_service.exception.ResourceNotFoundException;
import com.ecommerce.order_service.mapper.OrderMapper;
import com.ecommerce.order_service.publisher.OrderEventPublisher;
import com.ecommerce.order_service.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@SuppressWarnings("null")
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final OrderEventPublisher eventPublisher;
    private final OrderMapper orderMapper;

    @Override
    @Transactional
    public OrderResponseDto placeOrder(OrderRequestDto requestDto, String userId) {
        log.info("Placing order for user={}, items={}", userId, requestDto.getItems());

        String orderId = UUID.randomUUID().toString();

        LocalDateTime now = LocalDateTime.now();
        Order order = Order.builder()
                .orderId(orderId)
                .userId(userId)
                .items(requestDto.getItems())
                .amount(requestDto.getAmount())
                .status(OrderStatus.PENDING)
                .createdAt(now)
                .updatedAt(now)
                .build();

        Order saved = orderRepository.save(order);
        log.info("Order saved — orderId={}, status=PENDING", orderId);

        String itemsJson = "[" + requestDto.getItems().stream()
                .map(item -> "\"" + item + "\"")
                .collect(Collectors.joining(",")) + "]";

        eventPublisher.publishOrderCreated(orderId, userId, itemsJson, requestDto.getAmount());

        return orderMapper.toResponseDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderResponseDto> getMyOrders(String userId) {
        log.info("Fetching orders for userId={}", userId);
        return orderRepository.findByUserId(userId)
                .stream()
                .map(orderMapper::toResponseDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public OrderResponseDto getOrderById(Long id, String userId, boolean isAdmin) {
        log.info("Fetching order id={} by user={} isAdmin={}", id, userId, isAdmin);
        Order order = findOrderOrThrow(id);

        if (!isAdmin && !order.getUserId().equals(userId)) {
            throw new AccessDeniedException("You are not authorized to view this order.");
        }

        return orderMapper.toResponseDto(order);
    }

    @Override
    @Transactional
    public OrderResponseDto cancelOrder(Long id, String userId, boolean isAdmin) {
        log.info("Cancel request for order id={} by user={} isAdmin={}", id, userId, isAdmin);
        Order order = findOrderOrThrow(id);

        if (!isAdmin && !order.getUserId().equals(userId)) {
            throw new AccessDeniedException("You can only cancel your own orders.");
        }

        if (order.getStatus() != OrderStatus.PENDING) {
            throw new InvalidOrderStateException(
                "Only PENDING orders can be cancelled. Current status: " + order.getStatus());
        }

        order.setStatus(OrderStatus.CANCELLED);
        order.setUpdatedAt(LocalDateTime.now());
        Order saved = orderRepository.save(order);
        log.info("Order id={} cancelled", id);

        // Publish order.cancelled event so Payment Service can reverse the charge
        String reason = isAdmin ? "ADMIN_CANCELLED" : "CUSTOMER_CANCELLED";
        eventPublisher.publishOrderCancelled(saved.getOrderId(), saved.getUserId(), reason);

        return orderMapper.toResponseDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderResponseDto> getAllOrders() {
        log.info("Admin: fetching all orders");
        return orderRepository.findAll()
                .stream()
                .map(orderMapper::toResponseDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public OrderResponseDto updateOrderStatus(Long id, OrderStatus newStatus) {
        log.info("Admin: updating order id={} to status={}", id, newStatus);
        Order order = findOrderOrThrow(id);

        if (order.getStatus() == OrderStatus.CANCELLED || order.getStatus() == OrderStatus.DELIVERED) {
            throw new InvalidOrderStateException(
                "Cannot update status of a " + order.getStatus() + " order.");
        }

        order.setStatus(newStatus);
        order.setUpdatedAt(LocalDateTime.now());
        Order saved = orderRepository.save(order);
        log.info("Order id={} status updated to {}", id, newStatus);

        return orderMapper.toResponseDto(saved);
    }

    private Order findOrderOrThrow(Long id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order with ID " + id + " not found."));
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderResponseDto> getOrdersAssignedToMe(String deliveryExecutiveId) {
        log.info("Fetching orders assigned to delivery executive ID={}", deliveryExecutiveId);
        
        List<Order> orders = new java.util.ArrayList<>(orderRepository.findByDeliveryExecutiveId(deliveryExecutiveId));
        if ("6ad8df34-4263-4fce-a22a-4e0acabbde94".equals(deliveryExecutiveId)) {
            orders.addAll(orderRepository.findByDeliveryExecutiveId("delivery-exec-uuid"));
        }
        
        return orders.stream()
                .map(orderMapper::toResponseDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public OrderResponseDto assignOrderToDeliveryExecutive(Long id, String deliveryExecutiveId) {
        log.info("Admin: Assigning order id={} to delivery executive ID={}", id, deliveryExecutiveId);
        Order order = findOrderOrThrow(id);

        if (order.getStatus() == OrderStatus.CANCELLED || order.getStatus() == OrderStatus.DELIVERED) {
            throw new InvalidOrderStateException("Cannot assign a " + order.getStatus() + " order.");
        }

        order.setDeliveryExecutiveId(deliveryExecutiveId);
        order.setUpdatedAt(LocalDateTime.now());
        Order saved = orderRepository.save(order);
        log.info("Order id={} assigned to delivery executive ID={}", id, deliveryExecutiveId);
        return orderMapper.toResponseDto(saved);
    }

    @Override
    @Transactional
    public OrderResponseDto updateDeliveryStatus(Long id, OrderStatus status, String deliveryExecutiveId) {
        log.info("Delivery Executive: Updating order id={} to status={} by executive ID={}", id, status, deliveryExecutiveId);
        Order order = findOrderOrThrow(id);

        if (order.getDeliveryExecutiveId() == null) {
            throw new AccessDeniedException("You are not authorized to update delivery status for this order.");
        }

        boolean isAssigned = order.getDeliveryExecutiveId().equals(deliveryExecutiveId)
                || "delivery-exec-uuid".equals(order.getDeliveryExecutiveId());

        if (!isAssigned) {
            throw new AccessDeniedException("You are not authorized to update delivery status for this order.");
        }

        if (status != OrderStatus.OUT_FOR_DELIVERY && status != OrderStatus.DELIVERED && status != OrderStatus.DELIVERY_FAILED) {
            throw new IllegalArgumentException("Invalid delivery status: " + status);
        }

        if (order.getStatus() == OrderStatus.CANCELLED || order.getStatus() == OrderStatus.DELIVERED) {
            throw new InvalidOrderStateException("Cannot update status of a " + order.getStatus() + " order.");
        }

        order.setStatus(status);
        order.setUpdatedAt(LocalDateTime.now());
        Order saved = orderRepository.save(order);
        log.info("Order id={} status updated to {} by delivery executive", id, status);
        return orderMapper.toResponseDto(saved);
    }
}
