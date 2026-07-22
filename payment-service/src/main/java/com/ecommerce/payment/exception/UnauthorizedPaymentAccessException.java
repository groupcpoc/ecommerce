package com.ecommerce.payment.exception;

public class UnauthorizedPaymentAccessException extends RuntimeException {
    public UnauthorizedPaymentAccessException(String id) {
        super("You do not have access to payment: " + id);
    }
}