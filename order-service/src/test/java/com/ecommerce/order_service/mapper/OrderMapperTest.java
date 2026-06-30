package com.ecommerce.order_service.mapper;

import com.ecommerce.order_service.dto.OrderResponseDto;
import com.ecommerce.order_service.entity.Order;
import com.ecommerce.order_service.enums.OrderStatus;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class OrderMapperTest {

    private final OrderMapper orderMapper = new OrderMapper();

    @Test
    void toResponseDto_Null_ReturnsNull() {
        assertNull(orderMapper.toResponseDto(null));
    }

    @Test
    void toResponseDto_ValidOrder_MapsCorrectly() {
        LocalDateTime now = LocalDateTime.now();
        Order order = Order.builder()
                .id(1L)
                .orderId("uuid-123")
                .userId("user-abc")
                .items(List.of("Item A", "Item B"))
                .amount(150.0)
                .status(OrderStatus.PENDING)
                .deliveryExecutiveId("exec-999")
                .createdAt(now)
                .updatedAt(now)
                .build();

        OrderResponseDto dto = orderMapper.toResponseDto(order);

        assertNotNull(dto);
        assertEquals(1L, dto.getId());
        assertEquals("uuid-123", dto.getOrderId());
        assertEquals("user-abc", dto.getUserId());
        assertEquals(List.of("Item A", "Item B"), dto.getItems());
        assertEquals(150.0, dto.getAmount());
        assertEquals(OrderStatus.PENDING, dto.getStatus());
        assertEquals("exec-999", dto.getDeliveryExecutiveId());
        assertEquals(now, dto.getCreatedAt());
        assertEquals(now, dto.getUpdatedAt());
    }

    @Test
    void toResponseDtoList_Null_ReturnsEmptyList() {
        assertTrue(orderMapper.toResponseDtoList(null).isEmpty());
    }

    @Test
    void toResponseDtoList_ValidList_MapsCorrectly() {
        Order order = Order.builder()
                .id(1L)
                .orderId("uuid-123")
                .userId("user-abc")
                .status(OrderStatus.PENDING)
                .build();

        List<OrderResponseDto> list = orderMapper.toResponseDtoList(List.of(order));

        assertEquals(1, list.size());
        assertEquals("uuid-123", list.get(0).getOrderId());
    }
}
