package com.ecommerce.paymentservice.service;

import com.ecommerce.events.*;
import com.ecommerce.paymentservice.dto.PaymentResponseDTO;
import java.util.List;

public interface PaymentService {
    void processPayment(OrderCreatedEvent event);

    List<PaymentResponseDTO> getMyPayments(String userId);

    public PaymentResponseDTO getPaymentById(String id, String userId);

    public List<PaymentResponseDTO> getAllPayments();

    public PaymentResponseDTO refundPayment(String id);

    public void handleOrderCancelled(OrderCancelledEvent event);
}