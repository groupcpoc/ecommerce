package com.ecommerce.order_service.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AssignOrderRequestDto {

    @NotBlank(message = "Delivery executive ID is required")
    private String deliveryExecutiveId;
}
