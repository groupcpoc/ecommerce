package com.ecommerce.payment.mapper;

import com.ecommerce.payment.dto.PaymentResponse;
import com.ecommerce.payment.entity.Payment;
import com.ecommerce.payment.entity.PaymentStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.Month;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("PaymentMapper")
class PaymentMapperTest {
    LocalDateTime fixedTime = LocalDateTime.of(
            2024,
            Month.JANUARY,
            1,
            10,
            30
    );

    @Test
    @DisplayName("maps every field from Payment entity to PaymentResponse correctly")
    void mapsAllFieldsCorrectly() {

        Payment payment = Payment.builder()
                .id("11111111-1111-1111-1111-111111111111")
                .orderId("order-123")
                .userId("user-456")
                .amount(BigDecimal.valueOf(499.99))
                .status(PaymentStatus.SUCCESS)
                .razorpayId("order_T7BxaqvauXjTC5")
                .failureReason(null)
                .createdAt(fixedTime)
                .updatedAt(fixedTime)
                .build();

        PaymentResponse result = PaymentMapper.toDTO(payment);

        assertThat(result.id()).isEqualTo("11111111-1111-1111-1111-111111111111");
        assertThat(result.orderId()).isEqualTo("order-123");
        assertThat(result.userId()).isEqualTo("user-456");
        assertThat(result.amount()).isEqualByComparingTo(BigDecimal.valueOf(499.99));
        assertThat(result.status()).isEqualTo(PaymentStatus.SUCCESS);
        assertThat(result.razorpayId()).isEqualTo("order_T7BxaqvauXjTC5");
        assertThat(result.failureReason()).isNull();
        assertThat(result.createdAt()).isEqualTo(fixedTime);
        assertThat(result.updatedAt()).isEqualTo(fixedTime);
    }

    @Test
    @DisplayName("maps a FAILED payment with a populated failureReason and null razorpayId")
    void mapsFailedPaymentCorrectly() {
        Payment payment = Payment.builder()
                .id("22222222-2222-2222-2222-222222222222")
                .orderId("order-999")
                .userId("user-999")
                .amount(BigDecimal.valueOf(100.00))
                .status(PaymentStatus.FAILED)
                .razorpayId(null)
                .failureReason("BAD_REQUEST_ERROR: amount must be no less than 0.")
                .createdAt(fixedTime)
                .updatedAt(fixedTime)
                .build();

        PaymentResponse result = PaymentMapper.toDTO(payment);

        assertThat(result.status()).isEqualTo(PaymentStatus.FAILED);
        assertThat(result.razorpayId()).isNull();
        assertThat(result.failureReason()).contains("amount must be no less than 0");
    }
}