package com.ecommerce.order_service.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.*;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderRequestDto {

    @NotEmpty(message = "Items list cannot be empty")
    private List<String> items;

    @NotNull(message = "Amount must be specified")
    @Positive(message = "Amount must be greater than zero")
    private Double amount;
}
