package com.ecommerce.authservice.kafka;

import com.ecommerce.authservice.model.RegisterRequest;
import com.ecommerce.authservice.model.SuspendedRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class KafkaProducerService {
    @Autowired
    private KafkaTemplate<String, Object> kafka;

    public void publish(RegisterRequest r, String role) {
        kafka.send("user.registered", r);
    }

    public void publishUserSuspended(SuspendedRequest event) {
        kafka.send("user.suspended",event);
    }
}
