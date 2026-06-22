package com.ecommerce.order_service.dto;

import com.ecommerce.order_service.enums.OrderStatus;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderResponseDto {

    private Long id;
    private String orderId;
    private String userId;
    private List<String> items;
    private Double amount;
    private OrderStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
