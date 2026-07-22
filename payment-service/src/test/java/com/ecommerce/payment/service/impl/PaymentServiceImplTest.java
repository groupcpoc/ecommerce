package com.ecommerce.payment.service.impl;

import com.ecommerce.events.OrderCancelledEvent;
import com.ecommerce.events.OrderCreatedEvent;
import com.ecommerce.payment.dto.PaymentResponse;
import com.ecommerce.payment.entity.Payment;
import com.ecommerce.payment.entity.PaymentStatus;
import com.ecommerce.payment.exception.PaymentNotEligibleException;
import com.ecommerce.payment.exception.PaymentNotFoundException;
import com.ecommerce.payment.exception.UnauthorizedPaymentAccessException;
import com.ecommerce.payment.publisher.PaymentEventPublisher;
import com.ecommerce.payment.repository.PaymentRepository;
import com.razorpay.Order;
import com.razorpay.OrderClient;
import com.razorpay.PaymentClient;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("PaymentServiceImpl")
class PaymentServiceImplTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private PaymentEventPublisher eventPublisher;

    @Mock
    private RazorpayClient razorpayClient;

    @Mock
    private OrderClient orderClient;

    @Mock
    private PaymentClient paymentClient;

    private PaymentServiceImpl paymentService;

    private static final String ORDER_ID = "order-123";
    private static final String USER_ID = "user-456";
    private static final String PAYMENT_ID = "11111111-1111-1111-1111-111111111111";
    private static final String RAZORPAY_ORDER_ID = "order_T7BxaqvauXjTC5";

    @BeforeEach
    void setUp() {
        razorpayClient.orders = orderClient;
        razorpayClient.payments = paymentClient;
        paymentService = new PaymentServiceImpl(paymentRepository, eventPublisher, razorpayClient);
    }

    private OrderCreatedEvent buildOrderCreatedEvent(String orderId, String userId, double amount) {
        OrderCreatedEvent event = mock(OrderCreatedEvent.class);
        when(event.getOrderId()).thenReturn(orderId);
        when(event.getUserId()).thenReturn(userId);
        when(event.getAmount()).thenReturn(amount);
        return event;
    }

    private OrderCancelledEvent buildOrderCancelledEvent(String orderId) {
        OrderCancelledEvent event = mock(OrderCancelledEvent.class);
        when(event.getOrderId()).thenReturn(orderId);
        return event;
    }

    private Payment buildPayment(String id, String orderId, String userId, PaymentStatus status, String razorpayId) {
        return Payment.builder()
                .id(id)
                .orderId(orderId)
                .userId(userId)
                .amount(BigDecimal.valueOf(499.99))
                .status(status)
                .razorpayId(razorpayId)
                .build();
    }

    // ---------------------------------------------------------------
    // processPayment(OrderCreatedEvent)
    // ---------------------------------------------------------------

    @Nested
    @DisplayName("processPayment")
    class ProcessPayment {

        @Test
        @DisplayName("does nothing when a payment already exists for the orderId (idempotency guard)")
        void skipsProcessingOnDuplicateOrderId(){
            OrderCreatedEvent event = mock(OrderCreatedEvent.class);
            when(event.getOrderId()).thenReturn(ORDER_ID);
            when(paymentRepository.existsByOrderId(ORDER_ID)).thenReturn(true);

            paymentService.processPayment(event);

            verify(paymentRepository, never()).save(any());
            verify(eventPublisher, never()).publishPaymentProcessed(any());
            verify(eventPublisher, never()).publishPaymentFailed(any(), anyString());
            verifyNoInteractions(orderClient);
        }

        @Test
        @DisplayName("saves a SUCCESS payment and publishes payment.processed when Razorpay order creation succeeds")
        void savesSuccessPaymentWhenRazorpaySucceeds() throws RazorpayException {
            OrderCreatedEvent event = buildOrderCreatedEvent(ORDER_ID, USER_ID, 499.99);
            when(paymentRepository.existsByOrderId(ORDER_ID)).thenReturn(false);

            Order razorpayOrder = mock(Order.class);
            when(razorpayOrder.get("id")).thenReturn(RAZORPAY_ORDER_ID);
            when(orderClient.create(any(JSONObject.class))).thenReturn(razorpayOrder);

            paymentService.processPayment(event);

            ArgumentCaptor<Payment> captor = ArgumentCaptor.forClass(Payment.class);
            verify(paymentRepository).save(captor.capture());

            Payment saved = captor.getValue();
            assertThat(saved.getOrderId()).isEqualTo(ORDER_ID);
            assertThat(saved.getUserId()).isEqualTo(USER_ID);
            assertThat(saved.getStatus()).isEqualTo(PaymentStatus.SUCCESS);
            assertThat(saved.getRazorpayId()).isEqualTo(RAZORPAY_ORDER_ID);
            assertThat(saved.getAmount()).isEqualByComparingTo(BigDecimal.valueOf(499.99));

            verify(eventPublisher).publishPaymentProcessed(event);
            verify(eventPublisher, never()).publishPaymentFailed(any(), anyString());
        }

        @Test
        @DisplayName("rounds amount to paise correctly when building the Razorpay order request")
        void buildsCorrectPaiseAmountForRazorpay() throws RazorpayException, JSONException {
            OrderCreatedEvent event = buildOrderCreatedEvent(ORDER_ID, USER_ID, 19.995);
            when(paymentRepository.existsByOrderId(ORDER_ID)).thenReturn(false);

            Order razorpayOrder = mock(Order.class);
            when(razorpayOrder.get("id")).thenReturn(RAZORPAY_ORDER_ID);

            ArgumentCaptor<JSONObject> optionsCaptor = ArgumentCaptor.forClass(JSONObject.class);
            when(orderClient.create(optionsCaptor.capture())).thenReturn(razorpayOrder);

            paymentService.processPayment(event);

            // 19.995 * 100 = 1999.5, HALF_UP rounds to 2000 paise, not truncated to 1999
            assertThat(optionsCaptor.getValue().getInt("amount")).isEqualTo(2000);
            assertThat(optionsCaptor.getValue().getString("currency")).isEqualTo("INR");
            assertThat(optionsCaptor.getValue().getString("receipt")).isEqualTo(ORDER_ID);
        }

        @Test
        @DisplayName("saves a FAILED payment and publishes payment.failed when Razorpay throws")
        void savesFailedPaymentWhenRazorpayThrows() throws RazorpayException {
            OrderCreatedEvent event = buildOrderCreatedEvent(ORDER_ID, USER_ID, 499.99);
            when(paymentRepository.existsByOrderId(ORDER_ID)).thenReturn(false);
            when(orderClient.create(any(JSONObject.class)))
                    .thenThrow(new RazorpayException("BAD_REQUEST_ERROR: amount must be no less than 0."));

            paymentService.processPayment(event);

            ArgumentCaptor<Payment> captor = ArgumentCaptor.forClass(Payment.class);
            verify(paymentRepository).save(captor.capture());

            Payment saved = captor.getValue();
            assertThat(saved.getStatus()).isEqualTo(PaymentStatus.FAILED);
            assertThat(saved.getFailureReason()).contains("amount must be no less than 0");
            assertThat(saved.getRazorpayId()).isNull();

            verify(eventPublisher).publishPaymentFailed(eq(event), contains("amount must be no less than 0"));
            verify(eventPublisher, never()).publishPaymentProcessed(any());
        }
    }

    // ---------------------------------------------------------------
    // getMyPayments(String)
    // ---------------------------------------------------------------

    @Nested
    @DisplayName("getMyPayments")
    class GetMyPayments {

        @Test
        @DisplayName("returns all payments belonging to the given userId, mapped to DTOs")
        void returnsMappedPaymentsForUser() {
            Payment p1 = buildPayment(PAYMENT_ID, ORDER_ID, USER_ID, PaymentStatus.SUCCESS, RAZORPAY_ORDER_ID);
            when(paymentRepository.findByUserId(USER_ID)).thenReturn(List.of(p1));

            List<PaymentResponse> result = paymentService.getMyPayments(USER_ID);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).id()).isEqualTo(PAYMENT_ID);
            assertThat(result.get(0).userId()).isEqualTo(USER_ID);
        }

        @Test
        @DisplayName("returns an empty list when the user has no payments")
        void returnsEmptyListWhenNoPayments() {
            when(paymentRepository.findByUserId(USER_ID)).thenReturn(List.of());

            List<PaymentResponse> result = paymentService.getMyPayments(USER_ID);

            assertThat(result).isEmpty();
        }
    }

    // ---------------------------------------------------------------
    // getPaymentById(String userId, String paymentId)
    // ---------------------------------------------------------------

    @Nested
    @DisplayName("getPaymentById")
    class GetPaymentById {

        @Test
        @DisplayName("throws PaymentNotFoundException when the payment does not exist")
        void throwsNotFoundWhenMissing() {
            when(paymentRepository.findById(PAYMENT_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> paymentService.getPaymentById(USER_ID, PAYMENT_ID))
                    .isInstanceOf(PaymentNotFoundException.class);
        }

        @Test
        @DisplayName("throws UnauthorizedPaymentAccessException when the payment belongs to a different user (403, not 404)")
        void throwsUnauthorizedWhenWrongOwner() {
            Payment payment = buildPayment(PAYMENT_ID, ORDER_ID, "someone-else", PaymentStatus.SUCCESS, RAZORPAY_ORDER_ID);
            when(paymentRepository.findById(PAYMENT_ID)).thenReturn(Optional.of(payment));

            assertThatThrownBy(() -> paymentService.getPaymentById(USER_ID, PAYMENT_ID))
                    .isInstanceOf(UnauthorizedPaymentAccessException.class);
        }

        @Test
        @DisplayName("returns the mapped payment when the requesting user is the owner")
        void returnsPaymentForOwner() {
            Payment payment = buildPayment(PAYMENT_ID, ORDER_ID, USER_ID, PaymentStatus.SUCCESS, RAZORPAY_ORDER_ID);
            when(paymentRepository.findById(PAYMENT_ID)).thenReturn(Optional.of(payment));

            PaymentResponse result = paymentService.getPaymentById(USER_ID, PAYMENT_ID);

            assertThat(result.id()).isEqualTo(PAYMENT_ID);
            assertThat(result.userId()).isEqualTo(USER_ID);
        }
    }

    // ---------------------------------------------------------------
    // getAllPayments()
    // ---------------------------------------------------------------

    @Nested
    @DisplayName("getAllPayments")
    class GetAllPayments {

        @Test
        @DisplayName("returns every payment in the repository, mapped to DTOs")
        void returnsAllMappedPayments() {
            Payment p1 = buildPayment(PAYMENT_ID, ORDER_ID, USER_ID, PaymentStatus.SUCCESS, RAZORPAY_ORDER_ID);
            Payment p2 = buildPayment("22222222-2222-2222-2222-222222222222", "order-999", "user-999",
                    PaymentStatus.FAILED, null);
            when(paymentRepository.findAll()).thenReturn(List.of(p1, p2));

            List<PaymentResponse> result = paymentService.getAllPayments();

            assertThat(result).hasSize(2);
        }
    }

    // ---------------------------------------------------------------
    // processRefund(String paymentId)
    // ---------------------------------------------------------------

    @Nested
    @DisplayName("processRefund")
    class ProcessRefund {

        @Test
        @DisplayName("throws PaymentNotFoundException when the payment does not exist")
        void throwsNotFoundWhenMissing() {
            when(paymentRepository.findById(PAYMENT_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> paymentService.processRefund(PAYMENT_ID))
                    .isInstanceOf(PaymentNotFoundException.class);

            verify(paymentRepository, never()).save(any());
        }

        @Test
        @DisplayName("throws PaymentNotEligibleException when status is not REFUND_REQUESTED")
        void throwsNotEligibleWhenWrongStatus() {
            Payment payment = buildPayment(PAYMENT_ID, ORDER_ID, USER_ID, PaymentStatus.SUCCESS, RAZORPAY_ORDER_ID);
            when(paymentRepository.findById(PAYMENT_ID)).thenReturn(Optional.of(payment));

            assertThatThrownBy(() -> paymentService.processRefund(PAYMENT_ID))
                    .isInstanceOf(PaymentNotEligibleException.class)
                    .hasMessageContaining("REFUND_REQUESTED");

            verify(paymentRepository, never()).save(any());
            verify(eventPublisher, never()).publishRefundProcessed(any());
        }

        @Test
        @DisplayName("sets status to REFUNDED and publishes refund.processed when payment is REFUND_REQUESTED")
        void marksRefundedWhenEligible() {
            Payment payment = buildPayment(PAYMENT_ID, ORDER_ID, USER_ID, PaymentStatus.REFUND_REQUESTED, RAZORPAY_ORDER_ID);
            when(paymentRepository.findById(PAYMENT_ID)).thenReturn(Optional.of(payment));

            PaymentResponse result = paymentService.processRefund(PAYMENT_ID);

            assertThat(result.status()).isEqualTo(PaymentStatus.REFUNDED);
            assertThat(payment.getStatus()).isEqualTo(PaymentStatus.REFUNDED);
            verify(paymentRepository).save(payment);
            verify(eventPublisher).publishRefundProcessed(payment);
            verifyNoInteractions(paymentClient);
        }
    }

    // ---------------------------------------------------------------
    // handleOrderCancelled(OrderCancelledEvent)
    // ---------------------------------------------------------------

    @Nested
    @DisplayName("handleOrderCancelled")
    class HandleOrderCancelled {

        @Test
        @DisplayName("does nothing when no payment exists for the orderId")
        void noOpWhenPaymentNotFound() {
            OrderCancelledEvent event = buildOrderCancelledEvent(ORDER_ID);
            when(paymentRepository.findByOrderId(ORDER_ID)).thenReturn(Optional.empty());

            paymentService.handleOrderCancelled(event);

            verify(paymentRepository, never()).save(any());
        }

        @Test
        @DisplayName("does nothing when the matching payment is not in SUCCESS status")
        void noOpWhenStatusIsNotSuccess() {
            OrderCancelledEvent event = buildOrderCancelledEvent(ORDER_ID);
            Payment payment = buildPayment(PAYMENT_ID, ORDER_ID, USER_ID, PaymentStatus.FAILED, null);
            when(paymentRepository.findByOrderId(ORDER_ID)).thenReturn(Optional.of(payment));

            paymentService.handleOrderCancelled(event);

            assertThat(payment.getStatus()).isEqualTo(PaymentStatus.FAILED);
            verify(paymentRepository, never()).save(any());
        }

        @Test
        @DisplayName("flips a SUCCESS payment to REFUND_REQUESTED")
        void flipsSuccessPaymentToRefundRequested() {
            OrderCancelledEvent event = buildOrderCancelledEvent(ORDER_ID);
            Payment payment = buildPayment(PAYMENT_ID, ORDER_ID, USER_ID, PaymentStatus.SUCCESS, RAZORPAY_ORDER_ID);
            when(paymentRepository.findByOrderId(ORDER_ID)).thenReturn(Optional.of(payment));

            paymentService.handleOrderCancelled(event);

            assertThat(payment.getStatus()).isEqualTo(PaymentStatus.REFUND_REQUESTED);
            verify(paymentRepository).save(payment);
        }
    }
}