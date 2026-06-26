package com.ecommerce.paymentservice.mapper;

import com.ecommerce.paymentservice.dto.PaymentResponseDTO;
import com.ecommerce.paymentservice.entity.Payment;

public class PaymentMapper {

    private PaymentMapper() {
        // utility class, prevent instantiation
    }

    public static PaymentResponseDTO toDTO(Payment payment) {
        return new PaymentResponseDTO(
                payment.getId(),
                payment.getOrderId(),
                payment.getUserId(),
                payment.getAmount(),
                payment.getStatus(),
                payment.getRazorpayPaymentId(),
                payment.getFailureReason(),
                payment.getCreatedAt(),
                payment.getUpdatedAt());
    }
}