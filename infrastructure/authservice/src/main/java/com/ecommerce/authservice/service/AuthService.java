package com.ecommerce.authservice.service;

import com.ecommerce.authservice.kafka.KafkaProducerService;
import com.ecommerce.authservice.model.LoginRequest;
import com.ecommerce.authservice.model.RefreshRequest;
import com.ecommerce.authservice.model.RegisterRequest;
import com.ecommerce.authservice.model.TokenResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class AuthService {
    @Autowired
    private KeycloakService k;
    @Autowired
    private KafkaProducerService kafka;

    public String registerAdmin(RegisterRequest r, String role) {
        k.createUser(r,"admin");
        kafka.publish(r,"admin");
        return " Admin User Registered";
    }
    public String registerUser(RegisterRequest r, String role) {
        k.createUser(r,"user");
        kafka.publish(r,"user");
        return "User Registered";
    }
    public TokenResponse login(LoginRequest r) {
        return k.login(r);
    }

    public TokenResponse refresh(RefreshRequest r) {
        return k.refresh(r.refresh_token);
    }
}
