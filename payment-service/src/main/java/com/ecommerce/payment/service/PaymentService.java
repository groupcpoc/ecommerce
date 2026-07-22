package com.ecommerce.payment.service;

import com.ecommerce.events.*;
import com.ecommerce.payment.dto.PaymentResponse;
import java.util.List;

public interface PaymentService {
    void processPayment(OrderCreatedEvent event);

    List<PaymentResponse> getMyPayments(String userId);

    PaymentResponse getPaymentById(String userId, String paymentId);

    List<PaymentResponse> getAllPayments();

    PaymentResponse processRefund(String paymentId);

    void handleOrderCancelled(OrderCancelledEvent event);
}