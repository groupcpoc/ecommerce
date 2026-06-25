package com.ecommerce.paymentservice.controller;

import com.ecommerce.paymentservice.dto.PaymentResponseDTO;
import com.ecommerce.paymentservice.entity.PaymentStatus;
import com.ecommerce.paymentservice.exception.PaymentNotFoundException;
import com.ecommerce.paymentservice.service.PaymentService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PaymentController.class)
class PaymentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PaymentService paymentService;

    @Test
    void getMyPayments_returnsListForUser() throws Exception {
        PaymentResponseDTO dto = sampleDto();
        when(paymentService.getMyPayments("user-1")).thenReturn(List.of(dto));

        mockMvc.perform(get("/api/payments/me").header("X-User-Id", "user-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("p1"))
                .andExpect(jsonPath("$[0].status").value("SUCCESS"));
    }

    @Test
    void getPaymentById_whenFound_returnsPayment() throws Exception {
        PaymentResponseDTO dto = sampleDto();
        when(paymentService.getPaymentById("p1", "user-1")).thenReturn(dto);

        mockMvc.perform(get("/api/payments/p1").header("X-User-Id", "user-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderId").value("order-1"));
    }

    @Test
    void getPaymentById_whenNotFound_returns404() throws Exception {
        when(paymentService.getPaymentById("missing", "user-1"))
                .thenThrow(new PaymentNotFoundException("missing"));

        mockMvc.perform(get("/api/payments/missing").header("X-User-Id", "user-1"))
                .andExpect(status().isNotFound());
    }

    @Test
    void getAllPayments_returnsAll() throws Exception {
        when(paymentService.getAllPayments()).thenReturn(List.of(sampleDto()));

        mockMvc.perform(get("/api/payments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void refundPayment_returnsRefundedDto() throws Exception {
        PaymentResponseDTO refunded = new PaymentResponseDTO(
                "p1", "order-1", "user-1", 100.0, PaymentStatus.REFUNDED,
                "rzp_1", null, LocalDateTime.now(), LocalDateTime.now());
        when(paymentService.refundPayment("p1")).thenReturn(refunded);

        mockMvc.perform(post("/api/payments/p1/refund"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REFUNDED"));
    }

    private PaymentResponseDTO sampleDto() {
        return new PaymentResponseDTO(
                "p1", "order-1", "user-1", 100.0, PaymentStatus.SUCCESS,
                "rzp_1", null, LocalDateTime.now(), LocalDateTime.now());
    }
}