package com.ecommerce.authservice.kafka;

import com.ecommerce.authservice.model.RegisterRequest;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

@Service
public class KafkaConsumerService {
//    private static final Logger log = (Logger) LogFactory.getLog(KafkaConsumerService.class);
    List<RegisterRequest> registerRequestList = new ArrayList<>();

    @KafkaListener(topics = "user-fetch", groupId = "user.register")
    public void consume(RegisterRequest request) {

//        log.info("userlist" + request);
        registerRequestList.add(request);
//        log.info("Message from event");
    }

    public List<RegisterRequest> getUserFromConsumer() {
        return registerRequestList;
    }
}
