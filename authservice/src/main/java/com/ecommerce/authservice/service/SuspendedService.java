package com.ecommerce.authservice.service;

import com.ecommerce.authservice.kafka.KafkaProducerService;
import com.ecommerce.authservice.model.SuspendedRequest;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
@Service
public class SuspendedService {
    private final KeycloakService keycloakService;
    private final KafkaProducerService kafkaProducerService;

    public SuspendedService(KeycloakService keycloakService, KafkaProducerService kafkaProducerService) {
        this.keycloakService = keycloakService;
        this.kafkaProducerService = kafkaProducerService;
    }

    public void suspendUser(String userId) {

        keycloakService.disableUser(userId);
        keycloakService.revokeUserSessions(userId);

        SuspendedRequest event = new SuspendedRequest(
                userId,
                "SUSPENDED",
                LocalDateTime.now());

        kafkaProducerService.publishUserSuspended(event);}

}
