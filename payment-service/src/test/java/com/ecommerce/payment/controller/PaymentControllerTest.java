package com.ecommerce.payment.controller;

import com.ecommerce.payment.dto.PaymentResponse;
import com.ecommerce.payment.entity.PaymentStatus;
import com.ecommerce.payment.exception.PaymentNotEligibleException;
import com.ecommerce.payment.exception.PaymentNotFoundException;
import com.ecommerce.payment.exception.UnauthorizedPaymentAccessException;
import com.ecommerce.payment.service.PaymentService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PaymentController.class)
@DisplayName("PaymentController")
class PaymentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PaymentService paymentService;

    private static final String USER_ID = "user-456";
    private static final String PAYMENT_ID = "11111111-1111-1111-1111-111111111111";

    private PaymentResponse buildResponse(PaymentStatus status) {
        return new PaymentResponse(
                PAYMENT_ID, "order-123", USER_ID, BigDecimal.valueOf(499.99),
                status, "order_T7BxaqvauXjTC5", null,
                LocalDateTime.now(), LocalDateTime.now());
    }

    @Nested
    @DisplayName("GET /api/payments/me")
    class GetMyPayments {

        @Test
        @DisplayName("returns 200 with the caller's payments wrapped in ApiResponse")
        void returnsOwnPayments() throws Exception {
            when(paymentService.getMyPayments(USER_ID))
                    .thenReturn(List.of(buildResponse(PaymentStatus.SUCCESS)));

            mockMvc.perform(get("/api/payments/me")
                            .header("X-User-Id", USER_ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data[0].userId").value(USER_ID))
                    .andExpect(jsonPath("$.data[0].status").value("SUCCESS"));
        }
    }

    @Nested
    @DisplayName("GET /api/payments/{id}")
    class GetPaymentById {

        @Test
        @DisplayName("returns 200 with the payment when the caller is the owner")
        void returnsPaymentForOwner() throws Exception {
            when(paymentService.getPaymentById(USER_ID, PAYMENT_ID))
                    .thenReturn(buildResponse(PaymentStatus.SUCCESS));

            mockMvc.perform(get("/api/payments/{id}", PAYMENT_ID)
                            .header("X-User-Id", USER_ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.id").value(PAYMENT_ID));
        }

        @Test
        @DisplayName("returns 404 when the payment does not exist")
        void returns404WhenNotFound() throws Exception {
            when(paymentService.getPaymentById(USER_ID, PAYMENT_ID))
                    .thenThrow(new PaymentNotFoundException(PAYMENT_ID));

            mockMvc.perform(get("/api/payments/{id}", PAYMENT_ID)
                            .header("X-User-Id", USER_ID))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("returns 403 when the payment belongs to a different user")
        void returns403WhenWrongOwner() throws Exception {
            when(paymentService.getPaymentById(USER_ID, PAYMENT_ID))
                    .thenThrow(new UnauthorizedPaymentAccessException(PAYMENT_ID));

            mockMvc.perform(get("/api/payments/{id}", PAYMENT_ID)
                            .header("X-User-Id", USER_ID))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("GET /api/payments")
    class GetAllPayments {

        @Test
        @DisplayName("returns 200 with all payments")
        void returnsAllPayments() throws Exception {
            when(paymentService.getAllPayments())
                    .thenReturn(List.of(
                            buildResponse(PaymentStatus.SUCCESS),
                            buildResponse(PaymentStatus.FAILED)));

            mockMvc.perform(get("/api/payments"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.length()").value(2));
        }
    }

    @Nested
    @DisplayName("POST /api/payments/{id}/refund")
    class RefundPayment {

        @Test
        @DisplayName("returns 200 with REFUNDED status on successful refund")
        void returns200OnSuccessfulRefund() throws Exception {
            when(paymentService.processRefund(PAYMENT_ID))
                    .thenReturn(buildResponse(PaymentStatus.REFUNDED));

            mockMvc.perform(post("/api/payments/{id}/refund", PAYMENT_ID)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.status").value("REFUNDED"));
        }

        @Test
        @DisplayName("returns 404 when the payment does not exist")
        void returns404WhenNotFound() throws Exception {
            when(paymentService.processRefund(PAYMENT_ID))
                    .thenThrow(new PaymentNotFoundException(PAYMENT_ID));

            mockMvc.perform(post("/api/payments/{id}/refund", PAYMENT_ID))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("returns 400 when the payment is not eligible for refund")
        void returns400WhenNotEligible() throws Exception {
            when(paymentService.processRefund(PAYMENT_ID))
                    .thenThrow(new PaymentNotEligibleException("Only REFUND_REQUESTED payments can be refunded"));

            mockMvc.perform(post("/api/payments/{id}/refund", PAYMENT_ID))
                    .andExpect(status().isBadRequest());
        }
    }
}