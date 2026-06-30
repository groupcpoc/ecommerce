package com.ecommerce.inventoryservice.repository;

import java.util.List;
import java.util.Optional;

import com.ecommerce.inventoryservice.entity.InventoryItem;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InventoryItemRepository extends JpaRepository<InventoryItem, Long> {

    Optional<InventoryItem> findByProductId(String productId);

    boolean existsByProductId(String productId);

    List<InventoryItem> findAllByOrderByProductIdAsc();
}
