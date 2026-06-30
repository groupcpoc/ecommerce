package com.ecommerce.product;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.springframework.boot.SpringApplication;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class ProductServiceApplicationTest {

    @Test
    void mainShouldCallSpringApplicationRun() {
        try (MockedStatic<SpringApplication> mocked = Mockito.mockStatic(SpringApplication.class)) {
            mocked.when(() -> SpringApplication.run(ProductServiceApplication.class, new String[]{}))
                    .thenReturn(null);

            assertDoesNotThrow(() -> ProductServiceApplication.main(new String[]{}));

            mocked.verify(() -> SpringApplication.run(ProductServiceApplication.class, new String[]{}));
        }
    }

    @Test
    void mainShouldHandleArgs() {
        String[] args = {"--server.port=8084"};

        try (MockedStatic<SpringApplication> mocked = Mockito.mockStatic(SpringApplication.class)) {
            mocked.when(() -> SpringApplication.run(ProductServiceApplication.class, args))
                    .thenReturn(null);

            assertDoesNotThrow(() -> ProductServiceApplication.main(args));

            mocked.verify(() -> SpringApplication.run(ProductServiceApplication.class, args));
        }
    }
}