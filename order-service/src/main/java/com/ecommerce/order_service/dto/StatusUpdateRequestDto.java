package com.ecommerce.order_service.dto;

import com.ecommerce.order_service.enums.OrderStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class StatusUpdateRequestDto {

    @NotNull(message = "Target status must be provided")
    private OrderStatus status;
}
