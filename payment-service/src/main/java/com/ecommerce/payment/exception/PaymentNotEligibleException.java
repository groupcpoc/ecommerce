package com.ecommerce.payment.exception;

public class PaymentNotEligibleException extends RuntimeException {
    public PaymentNotEligibleException(String message) {
        super(message);
    }
}