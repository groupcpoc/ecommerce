package com.ecommerce.product.dto;

import com.ecommerce.product.enums.ProductStatus;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductResponse {
    private Long id;
    private String name;
    private String description;
    private Double price;
    private Integer quantity;
    private String category;
    private String sku;
    private ProductStatus status;
}