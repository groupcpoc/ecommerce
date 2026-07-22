package com.ecommerce.payment.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("GlobalExceptionHandler")
class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    @DisplayName("maps PaymentNotFoundException to 404")
    void mapsNotFoundTo404() {
        ResponseEntity<ErrorResponse> response =
                handler.handleNotFound(new PaymentNotFoundException("abc-123"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody().message()).contains("abc-123");
    }

    @Test
    @DisplayName("maps UnauthorizedPaymentAccessException to 403")
    void mapsUnauthorizedTo403() {
        ResponseEntity<ErrorResponse> response =
                handler.handleUnauthorizedAccess(new UnauthorizedPaymentAccessException("abc-123"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    @DisplayName("maps PaymentNotEligibleException to 400")
    void mapsNotEligibleTo400() {
        ResponseEntity<ErrorResponse> response =
                handler.handleNotEligible(new PaymentNotEligibleException("Only REFUND_REQUESTED payments can be refunded"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    @DisplayName("maps a generic Exception to 500 with a safe message")
    void mapsGenericExceptionTo500() {
        ResponseEntity<ErrorResponse> response =
                handler.handleGeneric(new RuntimeException("some internal detail that shouldn't leak"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody().message()).isEqualTo("Something went wrong");
    }
}