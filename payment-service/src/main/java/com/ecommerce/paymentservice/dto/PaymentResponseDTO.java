package com.ecommerce.paymentservice.dto;

import com.ecommerce.paymentservice.entity.PaymentStatus;

import java.time.LocalDateTime;

public record PaymentResponseDTO(String id, String orderId, String userId, Double amount, PaymentStatus status,
                String razorpayPaymentId, String failureReason, LocalDateTime createdAt, LocalDateTime updatedAt) {
}