package com.ecommerce.paymentservice.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "payments")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    private String orderId;
    private String userId;
    private Double amount;

    @Enumerated(EnumType.STRING)
    @Column(columnDefinition = "VARCHAR(255)")
    private PaymentStatus status;

    private String razorpayPaymentId;
    private String failureReason;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}