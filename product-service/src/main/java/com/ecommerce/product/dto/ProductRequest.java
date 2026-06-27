package com.ecommerce.product.dto;

import com.ecommerce.product.enums.ProductStatus;
import jakarta.validation.constraints.*;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductRequest {

    @NotBlank
    @Size(max = 200)
    private String name;

    @Size(max = 500)
    private String description;

    @NotNull
    @DecimalMin("0.0")
    private Double price;

    @NotNull
    @Min(0)
    private Integer quantity;

    @NotBlank
    @Size(max = 50)
    private String category;

    @NotBlank
    @Size(max = 100)
    private String sku;

    @NotNull
    private ProductStatus status;
}