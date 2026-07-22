package com.ecommerce.payment.controller;

import com.ecommerce.payment.dto.ApiResponse;
import com.ecommerce.payment.dto.PaymentResponse;
import com.ecommerce.payment.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<List<PaymentResponse>>> getMyPayments(
            @RequestHeader("X-User-Id") String userId) {
        List<PaymentResponse> payments = paymentService.getMyPayments(userId);
        return ResponseEntity.ok(ApiResponse.success("Payments fetched", payments));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<PaymentResponse>> getPaymentById(
            @PathVariable String id,
            @RequestHeader("X-User-Id") String userId) {
        PaymentResponse payment = paymentService.getPaymentById(userId, id);
        return ResponseEntity.ok(ApiResponse.success("Payment fetched", payment));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<PaymentResponse>>> getAllPayments() {
        List<PaymentResponse> payments = paymentService.getAllPayments();
        return ResponseEntity.ok(ApiResponse.success("All payments fetched", payments));
    }

    @PostMapping("/{id}/refund")
    public ResponseEntity<ApiResponse<PaymentResponse>> refundPayment(@PathVariable String id) {
        PaymentResponse payment = paymentService.processRefund(id);
        return ResponseEntity.ok(ApiResponse.success("Refund processed", payment));
    }
}