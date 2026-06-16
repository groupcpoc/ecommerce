package com.ecommerce.inventoryservice.repository;

import java.util.Optional;

import com.ecommerce.inventoryservice.entity.ProcessedKafkaEvent;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProcessedKafkaEventRepository extends JpaRepository<ProcessedKafkaEvent, Long> {

    Optional<ProcessedKafkaEvent> findByEventId(String eventId);
}
