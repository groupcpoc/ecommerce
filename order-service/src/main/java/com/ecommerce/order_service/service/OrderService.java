package com.ecommerce.order_service.service;

import com.ecommerce.order_service.dto.OrderRequestDto;
import com.ecommerce.order_service.dto.OrderResponseDto;
import com.ecommerce.order_service.enums.OrderStatus;

import java.util.List;

public interface OrderService {

    OrderResponseDto placeOrder(OrderRequestDto requestDto, String userId);

    List<OrderResponseDto> getMyOrders(String userId);

    OrderResponseDto getOrderById(String orderId, String userId, boolean isAdmin);

    OrderResponseDto cancelOrder(String orderId, String userId, boolean isAdmin);

    List<OrderResponseDto> getAllOrders();

    OrderResponseDto updateOrderStatus(String orderId, OrderStatus newStatus);

    List<OrderResponseDto> getOrdersAssignedToMe(String deliveryExecutiveId);

    OrderResponseDto assignOrderToDeliveryExecutive(String orderId, String deliveryExecutiveId);

    OrderResponseDto updateDeliveryStatus(String orderId, OrderStatus status, String deliveryExecutiveId);
}
