package com.ecommerce.order_service.service;

import com.ecommerce.order_service.dto.OrderRequestDto;
import com.ecommerce.order_service.dto.OrderResponseDto;
import com.ecommerce.order_service.enums.OrderStatus;

import java.util.List;

public interface OrderService {

    OrderResponseDto placeOrder(OrderRequestDto requestDto, String userId);

    List<OrderResponseDto> getMyOrders(String userId);

    OrderResponseDto getOrderById(Long id, String userId, boolean isAdmin);

    OrderResponseDto cancelOrder(Long id, String userId, boolean isAdmin);

    List<OrderResponseDto> getAllOrders();

    OrderResponseDto updateOrderStatus(Long id, OrderStatus newStatus);
}
