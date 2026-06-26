package com.ecommerce.paymentservice.mapper;

import com.ecommerce.paymentservice.dto.PaymentResponseDTO;
import com.ecommerce.paymentservice.entity.Payment;
import com.ecommerce.paymentservice.entity.PaymentStatus;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.Month;

import static org.assertj.core.api.Assertions.assertThat;

class PaymentMapperTest {

    @Test
    void toDTO_mapsAllFieldsCorrectly() {
        LocalDateTime now = LocalDateTime.of(2026, Month.JANUARY, 1, 10, 0);
        Payment payment = Payment.builder()
                .id("p1")
                .orderId("order-1")
                .userId("user-1")
                .amount(250.0)
                .status(PaymentStatus.SUCCESS)
                .razorpayPaymentId("rzp_1")
                .failureReason(null)
                .createdAt(now)
                .updatedAt(now)
                .build();

        PaymentResponseDTO dto = PaymentMapper.toDTO(payment);

        assertThat(dto.id()).isEqualTo("p1");
        assertThat(dto.orderId()).isEqualTo("order-1");
        assertThat(dto.userId()).isEqualTo("user-1");
        assertThat(dto.amount()).isEqualTo(250.0);
        assertThat(dto.status()).isEqualTo(PaymentStatus.SUCCESS);
        assertThat(dto.razorpayPaymentId()).isEqualTo("rzp_1");
        assertThat(dto.createdAt()).isEqualTo(now);
    }
}