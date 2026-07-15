package com.ecommerce.payment.dto;

import com.ecommerce.payment.entity.PaymentStatus;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;



public record PaymentResponse(String id, String orderId, String userId, BigDecimal amount, PaymentStatus status,
                              String razorpayId, String failureReason, LocalDateTime createdAt, LocalDateTime updatedAt) {
}