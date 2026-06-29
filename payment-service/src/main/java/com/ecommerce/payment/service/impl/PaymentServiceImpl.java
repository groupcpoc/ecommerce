package com.ecommerce.payment.service.impl;

import com.ecommerce.events.*;
import com.ecommerce.payment.dto.PaymentResponse;
import com.ecommerce.payment.entity.Payment;
import com.ecommerce.payment.entity.PaymentStatus;
import com.ecommerce.payment.exception.PaymentNotEligibleException;
import com.ecommerce.payment.exception.PaymentNotFoundException;
import com.ecommerce.payment.exception.UnauthorizedPaymentAccessException;
import com.ecommerce.payment.mapper.PaymentMapper;
import com.ecommerce.payment.publisher.PaymentEventPublisher;
import com.ecommerce.payment.repository.PaymentRepository;
import com.ecommerce.payment.service.PaymentService;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentServiceImpl implements PaymentService {

    private static final BigDecimal PAISE_MULTIPLIER = BigDecimal.valueOf(100);

    private final PaymentRepository paymentRepository;
    private final PaymentEventPublisher eventPublisher;
    private final RazorpayClient razorpayClient;

    @Override
    public void processPayment(OrderCreatedEvent event) {
        if (paymentRepository.existsByOrderId(event.getOrderId().toString())) {
            log.warn("Duplicate payment attempt for orderId: {}", event.getOrderId());
            return;
        }

        BigDecimal amount = BigDecimal.valueOf(event.getAmount());

        try {
            JSONObject options = new JSONObject();
            options.put("amount", amount.multiply(PAISE_MULTIPLIER).setScale(0, RoundingMode.HALF_UP).intValue());
            options.put("currency", "INR");
            options.put("receipt", event.getOrderId().toString());

            com.razorpay.Order razorpayOrder = razorpayClient.orders.create(options);
            String razorpayId = razorpayOrder.get("id");

            Payment payment = Payment.builder()
                    .orderId(event.getOrderId().toString())
                    .userId(event.getUserId().toString())
                    .amount(amount)
                    .status(PaymentStatus.SUCCESS)
                    .razorpayId(razorpayId)
                    .createdAt(LocalDateTime.now(java.time.ZoneOffset.UTC))
                    .updatedAt(LocalDateTime.now(java.time.ZoneOffset.UTC))
                    .build();
            paymentRepository.save(payment);

            eventPublisher.publishPaymentProcessed(event);

        } catch (RazorpayException e) {
            log.error("Razorpay failed for orderId: {} — {}", event.getOrderId(), e.getMessage());

            Payment payment = Payment.builder()
                    .orderId(event.getOrderId().toString())
                    .userId(event.getUserId().toString())
                    .amount(amount)
                    .status(PaymentStatus.FAILED)
                    .failureReason(e.getMessage())
                    .createdAt(LocalDateTime.now(java.time.ZoneOffset.UTC))
                    .updatedAt(LocalDateTime.now(java.time.ZoneOffset.UTC))
                    .build();
            paymentRepository.save(payment);

            eventPublisher.publishPaymentFailed(event, e.getMessage());
        }
    }

    @Override
    public List<PaymentResponse> getMyPayments(String userId) {

        return paymentRepository.findByUserId(userId)
                .stream()
                .map(PaymentMapper::toDTO)
                .toList();
    }

    @Override
    public PaymentResponse getPaymentById(String userId, String paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new PaymentNotFoundException(paymentId));

        if (!payment.getUserId().equals(userId)) {
            throw new UnauthorizedPaymentAccessException(paymentId);
        }

        return PaymentMapper.toDTO(payment);
    }

    @Override
    public List<PaymentResponse> getAllPayments() {

        return paymentRepository.findAll()
                .stream()
                .map(PaymentMapper::toDTO)
                .toList();
    }

    @Override
    public PaymentResponse processRefund(String paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new PaymentNotFoundException(paymentId));

        if (payment.getStatus() != PaymentStatus.REFUND_REQUESTED) {
            throw new PaymentNotEligibleException("Only REFUND_REQUESTED payments can be refunded");
        }

        // Test mode — Razorpay does not generate a pay_ ID in test mode
        // Skipping actual Razorpay refund API call
        // razorpayClient.payments.refund(payment.getRazorpayId(), options);
        log.info("Test mode — skipping Razorpay refund API call for paymentId: {}", paymentId);

        payment.setStatus(PaymentStatus.REFUNDED);
        payment.setUpdatedAt(LocalDateTime.now(java.time.ZoneOffset.UTC));
        paymentRepository.save(payment);

        eventPublisher.publishRefundProcessed(payment);

        return PaymentMapper.toDTO(payment);
    }
    @Override
    public void handleOrderCancelled(OrderCancelledEvent event) {
        paymentRepository.findByOrderId(event.getOrderId().toString())
                .ifPresent(payment -> {
                    if (payment.getStatus() == PaymentStatus.SUCCESS) {
                        payment.setStatus(PaymentStatus.REFUND_REQUESTED);
                        payment.setUpdatedAt(LocalDateTime.now(java.time.ZoneOffset.UTC));
                        paymentRepository.save(payment);
                        log.info("Refund requested for orderId: {}", event.getOrderId());
                    }
                });
    }
}