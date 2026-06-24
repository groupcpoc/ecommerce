package com.ecommerce.authservice.kafka;

import com.ecommerce.authservice.model.RegisterRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
@Service
public class KafkaProducerService{
@Autowired private KafkaTemplate<String,Object> kafka;
public void publish(RegisterRequest r,String role){kafka.send("user.registered",r);} }
