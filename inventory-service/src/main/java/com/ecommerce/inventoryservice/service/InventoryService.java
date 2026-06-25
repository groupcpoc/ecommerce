package com.ecommerce.inventoryservice.service;

import java.util.List;

import com.ecommerce.inventoryservice.dto.InventoryCreateRequest;
import com.ecommerce.inventoryservice.dto.InventoryResponse;
import com.ecommerce.inventoryservice.dto.InventorySummaryResponse;
import com.ecommerce.inventoryservice.dto.InventoryUpdateRequest;
import com.ecommerce.inventoryservice.dto.OrderStockResponse;
import com.ecommerce.inventoryservice.dto.RestockRequest;
import com.ecommerce.inventoryservice.event.OrderCancelledEvent;
import com.ecommerce.inventoryservice.event.PaymentProcessedEvent;

public interface InventoryService {

    List<InventorySummaryResponse> getAllInventory();

    InventoryResponse getInventory(String productId);

    InventoryResponse createInventory(InventoryCreateRequest request);

    InventoryResponse updateInventory(String productId, InventoryUpdateRequest request);

    InventoryResponse restock(String productId, RestockRequest request);

    List<InventorySummaryResponse> getLowStockInventory();

    OrderStockResponse getOrderStock(String orderId);

    void processPaymentProcessedEvent(PaymentProcessedEvent event);

    void releaseInventoryForOrder(OrderCancelledEvent event);
}
