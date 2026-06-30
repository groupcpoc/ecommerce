package com.ecommerce.order_service.mapper;

import com.ecommerce.order_service.dto.OrderResponseDto;
import com.ecommerce.order_service.entity.Order;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class OrderMapper {

    public OrderResponseDto toResponseDto(Order order) {
        if (order == null) {
            return null;
        }

        return OrderResponseDto.builder()
                .id(order.getId())
                .orderId(order.getOrderId())
                .userId(order.getUserId())
                .items(order.getItems())
                .amount(order.getAmount())
                .status(order.getStatus())
                .deliveryExecutiveId(order.getDeliveryExecutiveId())
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .build();
    }

    public List<OrderResponseDto> toResponseDtoList(List<Order> orders) {
        if (orders == null) {
            return List.of();
        }
        return orders.stream()
                .map(this::toResponseDto)
                .collect(Collectors.toList());
    }
}
