package com.ecommerce.paymentservice.exception;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void handleNotFound_returns404WithMessage() {
        PaymentNotFoundException ex = new PaymentNotFoundException("p1");

        ResponseEntity<ErrorResponse> response = handler.handleNotFound(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody().message()).contains("p1");
    }

    @Test
    void handleInvalidState_returns400WithMessage() {
        InvalidPaymentStateException ex = new InvalidPaymentStateException("Invalid state");

        ResponseEntity<ErrorResponse> response = handler.handleInvalidState(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().message()).isEqualTo("Invalid state");
    }

    @Test
    void handleGeneric_returns500WithGenericMessage() {
        Exception ex = new RuntimeException("Unexpected boom");

        ResponseEntity<ErrorResponse> response = handler.handleGeneric(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody().message()).isEqualTo("Something went wrong");
    }
}