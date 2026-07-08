package com.ecommerce.authservice.service;


import com.ecommerce.authservice.kafka.KafkaConsumerService;
import com.ecommerce.authservice.kafka.KafkaProducerService;
import com.ecommerce.authservice.model.LoginRequest;
import com.ecommerce.authservice.model.RefreshRequest;
import com.ecommerce.authservice.model.RegisterRequest;
import com.ecommerce.authservice.model.TokenResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class AuthService {
    @Autowired
    private KeycloakService k;
    @Autowired
    private KafkaProducerService kafka;
    @Autowired
    private KafkaConsumerService kafkaConsumerService;
    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    public String registerAdmin(RegisterRequest r, String role) {
        k.createUser(r, "admin");
        kafka.publish(r, "admin");
        return " Admin User Registered";
    }

    public String userRegistration(RegisterRequest r, String role) {
        k.createUser(r, "user");
        kafka.publish(r, "user");
        return "User Registered";
    }

    public TokenResponse login(LoginRequest r) {
        return k.login(r);
    }

    public TokenResponse refresh(RefreshRequest r) {
        kafkaTemplate.send("user.notify",r.refresh_token);
        return k.refresh(r.refresh_token);
    }

    @KafkaListener(topics = "user-fetch", groupId = "user-service-group")
    public List<RegisterRequest> getAllUsers() {
        List<RegisterRequest> list = kafkaConsumerService.getUserFromConsumer();
        kafkaTemplate.send("user.notify","Fetching User List");
        return list.stream().sorted().collect(Collectors.toList());

    }

    public void deleteUser(Long id) {
        kafkaTemplate.send("user.deleted", "Deleted user id: " + id);
        kafkaTemplate.send("user.notify", "Deleted user id: " + id);
    }
}
