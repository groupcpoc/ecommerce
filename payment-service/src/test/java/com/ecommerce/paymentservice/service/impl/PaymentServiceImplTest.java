package com.ecommerce.paymentservice.service.impl;

import com.ecommerce.events.OrderCancelledEvent;
import com.ecommerce.events.OrderCreatedEvent;
import com.ecommerce.paymentservice.dto.PaymentResponseDTO;
import com.ecommerce.paymentservice.entity.Payment;
import com.ecommerce.paymentservice.entity.PaymentStatus;
import com.ecommerce.paymentservice.exception.InvalidPaymentStateException;
import com.ecommerce.paymentservice.exception.PaymentNotFoundException;
import com.ecommerce.paymentservice.publisher.PaymentEventPublisher;
import com.ecommerce.paymentservice.repository.PaymentRepository;
import com.razorpay.Order;
import com.razorpay.OrderClient;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceImplTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private PaymentEventPublisher eventPublisher;

    @Mock
    private RazorpayClient razorpayClient;

    @Mock
    private OrderClient orderClient;

    private PaymentServiceImpl paymentService;

    @BeforeEach
    void setUp() {
        paymentService = new PaymentServiceImpl(paymentRepository, eventPublisher, razorpayClient);
        // RazorpayClient.orders is a public field on the SDK object, not a getter — set
        // it directly
        razorpayClient.orders = orderClient;
    }

    // ---------- processPayment ----------

    @Test
    void processPayment_whenDuplicateOrder_skipsProcessing() {
        OrderCreatedEvent event = mock(OrderCreatedEvent.class);
        when(event.getOrderId()).thenReturn("order-1");
        when(paymentRepository.existsByOrderId("order-1")).thenReturn(true);

        paymentService.processPayment(event);

        verify(paymentRepository, never()).save(any());
        verifyNoInteractions(eventPublisher);
    }

    @Test
    void processPayment_whenRazorpaySucceeds_savesSuccessAndPublishesEvent() throws RazorpayException {
        OrderCreatedEvent event = mock(OrderCreatedEvent.class);
        when(event.getOrderId()).thenReturn("order-1");
        when(event.getUserId()).thenReturn("user-1");
        when(event.getAmount()).thenReturn(500.0);

        when(paymentRepository.existsByOrderId("order-1")).thenReturn(false);

        Order razorpayOrder = mock(Order.class);
        when(razorpayOrder.get("id")).thenReturn("rzp_order_123");
        when(orderClient.create(any(JSONObject.class))).thenReturn(razorpayOrder);

        paymentService.processPayment(event);

        verify(paymentRepository).save(argThat(payment -> payment.getOrderId().equals("order-1") &&
                payment.getStatus() == PaymentStatus.SUCCESS &&
                payment.getRazorpayPaymentId().equals("rzp_order_123")));
        verify(eventPublisher).publishPaymentProcessed(event);
        verify(eventPublisher, never()).publishPaymentFailed(any(), any());
    }

    @Test
    void processPayment_whenRazorpayThrows_savesFailedAndPublishesFailure() throws RazorpayException {
        OrderCreatedEvent event = mock(OrderCreatedEvent.class);
        when(event.getOrderId()).thenReturn("order-2");
        when(event.getUserId()).thenReturn("user-2");
        when(event.getAmount()).thenReturn(750.0);

        when(paymentRepository.existsByOrderId("order-2")).thenReturn(false);
        when(orderClient.create(any(JSONObject.class)))
                .thenThrow(new RazorpayException("Gateway timeout"));

        paymentService.processPayment(event);

        verify(paymentRepository).save(argThat(payment -> payment.getOrderId().equals("order-2") &&
                payment.getStatus() == PaymentStatus.FAILED &&
                payment.getFailureReason().equals("Gateway timeout")));
        verify(eventPublisher).publishPaymentFailed(event, "Gateway timeout");
        verify(eventPublisher, never()).publishPaymentProcessed(any());
    }

    // ---------- getMyPayments ----------

    @Test
    void getMyPayments_returnsMappedDTOs() {
        Payment payment = buildPayment("p1", "order-1", "user-1", PaymentStatus.SUCCESS);
        when(paymentRepository.findByUserId("user-1")).thenReturn(List.of(payment));

        List<PaymentResponseDTO> result = paymentService.getMyPayments("user-1");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).id()).isEqualTo("p1");
    }

    @Test
    void getMyPayments_whenNoPayments_returnsEmptyList() {
        when(paymentRepository.findByUserId("user-x")).thenReturn(List.of());

        List<PaymentResponseDTO> result = paymentService.getMyPayments("user-x");

        assertThat(result).isEmpty();
    }

    // ---------- getPaymentById ----------

    @Test
    void getPaymentById_whenFound_returnsDTO() {
        Payment payment = buildPayment("p1", "order-1", "user-1", PaymentStatus.SUCCESS);
        when(paymentRepository.findByIdAndUserId("p1", "user-1")).thenReturn(Optional.of(payment));

        PaymentResponseDTO result = paymentService.getPaymentById("p1", "user-1");

        assertThat(result.id()).isEqualTo("p1");
    }

    @Test
    void getPaymentById_whenNotFound_throwsPaymentNotFoundException() {
        when(paymentRepository.findByIdAndUserId("missing", "user-1")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> paymentService.getPaymentById("missing", "user-1"))
                .isInstanceOf(PaymentNotFoundException.class)
                .hasMessageContaining("missing");
    }

    // ---------- getAllPayments ----------

    @Test
    void getAllPayments_returnsAllMappedDTOs() {
        Payment p1 = buildPayment("p1", "o1", "u1", PaymentStatus.SUCCESS);
        Payment p2 = buildPayment("p2", "o2", "u2", PaymentStatus.FAILED);
        when(paymentRepository.findAll()).thenReturn(List.of(p1, p2));

        List<PaymentResponseDTO> result = paymentService.getAllPayments();

        assertThat(result).hasSize(2);
    }

    // ---------- refundPayment ----------

    @Test
    void refundPayment_whenRefundRequested_refundsAndPublishes() {
        Payment payment = buildPayment("p1", "o1", "u1", PaymentStatus.REFUND_REQUESTED);
        when(paymentRepository.findById("p1")).thenReturn(Optional.of(payment));

        PaymentResponseDTO result = paymentService.refundPayment("p1");

        assertThat(result.status()).isEqualTo(PaymentStatus.REFUNDED);
        verify(paymentRepository).save(argThat(p -> p.getStatus() == PaymentStatus.REFUNDED));
        verify(eventPublisher).publishRefundProcessed(payment);
    }

    @Test
    void refundPayment_whenNotRefundRequested_throwsInvalidPaymentStateException() {
        Payment payment = buildPayment("p1", "o1", "u1", PaymentStatus.SUCCESS);
        when(paymentRepository.findById("p1")).thenReturn(Optional.of(payment));

        assertThatThrownBy(() -> paymentService.refundPayment("p1"))
                .isInstanceOf(InvalidPaymentStateException.class);

        verify(paymentRepository, never()).save(any());
        verifyNoInteractions(eventPublisher);
    }

    @Test
    void refundPayment_whenPaymentNotFound_throwsPaymentNotFoundException() {
        when(paymentRepository.findById("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> paymentService.refundPayment("missing"))
                .isInstanceOf(PaymentNotFoundException.class);
    }

    // ---------- handleOrderCancelled ----------

    @Test
    void handleOrderCancelled_whenPaymentSuccessful_setsRefundRequested() {
        Payment payment = buildPayment("p1", "order-1", "user-1", PaymentStatus.SUCCESS);
        OrderCancelledEvent event = mock(OrderCancelledEvent.class);
        when(event.getOrderId()).thenReturn("order-1");
        when(paymentRepository.findByOrderId("order-1")).thenReturn(Optional.of(payment));

        paymentService.handleOrderCancelled(event);

        verify(paymentRepository).save(argThat(p -> p.getStatus() == PaymentStatus.REFUND_REQUESTED));
    }

    @Test
    void handleOrderCancelled_whenPaymentNotSuccessful_doesNothing() {
        Payment payment = buildPayment("p1", "order-1", "user-1", PaymentStatus.FAILED);
        OrderCancelledEvent event = mock(OrderCancelledEvent.class);
        when(event.getOrderId()).thenReturn("order-1");
        when(paymentRepository.findByOrderId("order-1")).thenReturn(Optional.of(payment));

        paymentService.handleOrderCancelled(event);

        verify(paymentRepository, never()).save(any());
    }

    @Test
    void handleOrderCancelled_whenPaymentNotFound_doesNothing() {
        OrderCancelledEvent event = mock(OrderCancelledEvent.class);
        when(event.getOrderId()).thenReturn("order-missing");
        when(paymentRepository.findByOrderId("order-missing")).thenReturn(Optional.empty());

        paymentService.handleOrderCancelled(event);

        verify(paymentRepository, never()).save(any());
    }

    // ---------- helper ----------

    private Payment buildPayment(String id, String orderId, String userId, PaymentStatus status) {
        return Payment.builder()
                .id(id)
                .orderId(orderId)
                .userId(userId)
                .amount(100.0)
                .status(status)
                .build();
    }
}