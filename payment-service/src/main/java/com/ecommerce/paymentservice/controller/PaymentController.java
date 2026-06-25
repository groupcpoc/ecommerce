package com.ecommerce.paymentservice.controller;

import com.ecommerce.paymentservice.dto.PaymentResponseDTO;
import com.ecommerce.paymentservice.entity.Payment;
import com.ecommerce.paymentservice.service.PaymentService;
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
    public ResponseEntity<List<PaymentResponseDTO>> getMyPayments(
            @RequestHeader("X-User-Id") String userId) {
        return ResponseEntity.ok(paymentService.getMyPayments(userId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<PaymentResponseDTO> getPaymentById(
            @PathVariable String id,
            @RequestHeader("X-User-Id") String userId) {
        return ResponseEntity.ok(paymentService.getPaymentById(id, userId));
    }

    @GetMapping
    public ResponseEntity<List<PaymentResponseDTO>> getAllPayments() {
        return ResponseEntity.ok(paymentService.getAllPayments());
    }

    @PostMapping("/{id}/refund")
    public ResponseEntity<PaymentResponseDTO> refundPayment(@PathVariable String id) {
        return ResponseEntity.ok(paymentService.refundPayment(id));
    }
}