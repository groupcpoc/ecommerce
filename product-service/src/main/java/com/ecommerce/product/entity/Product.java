package com.ecommerce.product.model;

import com.ecommerce.product.enums.ProductStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

@Entity
@Table(name = "products")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    @NotBlank
    @Size(max = 200)
    private String name;

    @Column(length = 500)
    @Size(max = 500)
    private String description;

    @Column(nullable = false)
    @NotNull
    @DecimalMin("0.0")
    private Double price;

    @Column(nullable = false)
    @NotNull
    @Min(0)
    private Integer quantity;

    @Column(nullable = false, length = 50)
    @NotBlank
    @Size(max = 50)
    private String category;

    @Column(nullable = false, unique = true, length = 100)
    @NotBlank
    @Size(max = 100)
    private String sku;

    @Column(nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private ProductStatus status;
}