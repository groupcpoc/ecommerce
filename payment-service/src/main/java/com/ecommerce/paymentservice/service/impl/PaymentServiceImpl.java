package com.ecommerce.paymentservice.service.impl;

import com.ecommerce.events.*;
import com.ecommerce.paymentservice.dto.PaymentResponseDTO;
import com.ecommerce.paymentservice.entity.Payment;
import com.ecommerce.paymentservice.entity.PaymentStatus;
import com.ecommerce.paymentservice.exception.InvalidPaymentStateException;
import com.ecommerce.paymentservice.exception.PaymentNotFoundException;
import com.ecommerce.paymentservice.mapper.PaymentMapper;
import com.ecommerce.paymentservice.publisher.PaymentEventPublisher;
import com.ecommerce.paymentservice.repository.PaymentRepository;
import com.ecommerce.paymentservice.service.PaymentService;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository paymentRepository;
    private final PaymentEventPublisher eventPublisher;
    private final RazorpayClient razorpayClient;

    @Override
    public void processPayment(OrderCreatedEvent event) {
        if (paymentRepository.existsByOrderId(event.getOrderId().toString())) {
            log.warn("Duplicate payment attempt for orderId: {}", event.getOrderId());
            return;
        }

        try {
            JSONObject options = new JSONObject();
            options.put("amount", (int) (event.getAmount() * 100));
            options.put("currency", "INR");
            options.put("receipt", event.getOrderId().toString());

            com.razorpay.Order razorpayOrder = razorpayClient.orders.create(options);
            String razorpayId = razorpayOrder.get("id");

            Payment payment = Payment.builder()
                    .orderId(event.getOrderId().toString())
                    .userId(event.getUserId().toString())
                    .amount(event.getAmount())
                    .status(PaymentStatus.SUCCESS)
                    .razorpayPaymentId(razorpayId)
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();
            paymentRepository.save(payment);

            eventPublisher.publishPaymentProcessed(event);

        } catch (RazorpayException e) {
            log.error("Razorpay failed for orderId: {} — {}", event.getOrderId(), e.getMessage());

            Payment payment = Payment.builder()
                    .orderId(event.getOrderId().toString())
                    .userId(event.getUserId().toString())
                    .amount(event.getAmount())
                    .status(PaymentStatus.FAILED)
                    .failureReason(e.getMessage())
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();
            paymentRepository.save(payment);

            eventPublisher.publishPaymentFailed(event, e.getMessage());
        }
    }

    @Override
    public List<PaymentResponseDTO> getMyPayments(String userId) {

        return paymentRepository.findByUserId(userId)
                .stream()
                .map(PaymentMapper::toDTO)
                .toList();
    }

    @Override
    public PaymentResponseDTO getPaymentById(String id, String userId) {
        Payment payment = paymentRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new PaymentNotFoundException(id));
        return PaymentMapper.toDTO(payment);
    }

    @Override
    public List<PaymentResponseDTO> getAllPayments() {

        return paymentRepository.findAll()
                .stream()
                .map(PaymentMapper::toDTO)
                .toList(); }

    @Override
    public PaymentResponseDTO refundPayment(String id) {
        Payment payment = paymentRepository.findById(id)
                .orElseThrow(() -> new PaymentNotFoundException(id));

        if (payment.getStatus() != PaymentStatus.REFUND_REQUESTED) {
            throw new InvalidPaymentStateException("Only REFUND_REQUESTED payments can be refunded");
        }

        payment.setStatus(PaymentStatus.REFUNDED);
        payment.setUpdatedAt(LocalDateTime.now());
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
                        payment.setUpdatedAt(LocalDateTime.now());
                        paymentRepository.save(payment);
                        log.info("Refund requested for orderId: {}", event.getOrderId());
                    }
                });
    }
}