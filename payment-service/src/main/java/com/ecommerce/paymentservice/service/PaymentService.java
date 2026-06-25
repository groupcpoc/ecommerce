package com.ecommerce.paymentservice.service;

import com.ecommerce.events.*;
import com.ecommerce.paymentservice.dto.PaymentResponseDTO;
import com.ecommerce.paymentservice.entity.Payment;
import com.ecommerce.paymentservice.entity.PaymentStatus;
import com.ecommerce.paymentservice.exception.InvalidPaymentStateException;
import com.ecommerce.paymentservice.exception.PaymentNotFoundException;
import com.ecommerce.paymentservice.repository.PaymentRepository;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

public interface PaymentService {
    void processPayment(OrderCreatedEvent event);

     List<PaymentResponseDTO > getMyPayments(String userId);

    public PaymentResponseDTO  getPaymentById(String id, String userId);

    public List<PaymentResponseDTO > getAllPayments();

    public PaymentResponseDTO refundPayment(String id);

    public void handleOrderCancelled(OrderCancelledEvent event);
}