package com.ecommerce.inventoryservice.repository;

import java.util.List;

import com.ecommerce.inventoryservice.entity.ReservationStatus;
import com.ecommerce.inventoryservice.entity.StockReservation;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StockReservationRepository extends JpaRepository<StockReservation, Long> {

    List<StockReservation> findByProductIdOrderByCreatedAtDesc(String productId);

    List<StockReservation> findByOrderIdOrderByCreatedAtDesc(String orderId);

    List<StockReservation> findByOrderIdAndStatusOrderByCreatedAtDesc(String orderId, ReservationStatus status);
}
