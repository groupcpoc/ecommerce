package com.ecommerce.inventoryservice.publisher;

import com.ecommerce.inventoryservice.event.InventoryEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(name = "app.kafka.enabled", havingValue = "true")
public class KafkaEventPublisherImpl implements KafkaEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(KafkaEventPublisherImpl.class);

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public KafkaEventPublisherImpl(KafkaTemplate<String, String> kafkaTemplate, ObjectMapper objectMapper) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public void publish(String topic, InventoryEvent event) {
        try {
            kafkaTemplate.send(topic, event.getEventId(), objectMapper.writeValueAsString(event))
                    .whenComplete((result, ex) -> {
                        if (ex != null) {
                            log.warn("Kafka publish failed for topic {}", topic, ex);
                        }
                    });
        } catch (JsonProcessingException ex) {
            log.warn("Failed to serialize inventory event for topic {}", topic, ex);
        } catch (Exception ex) {
            log.warn("Failed to publish inventory event for topic {}", topic, ex);
        }
    }
}
