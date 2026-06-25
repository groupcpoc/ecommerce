package com.ecommerce.inventoryservice;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ecommerce.inventoryservice.entity.InventoryItem;
import com.ecommerce.inventoryservice.entity.ReservationStatus;
import com.ecommerce.inventoryservice.entity.StockReservation;
import com.ecommerce.inventoryservice.repository.InventoryItemRepository;
import com.ecommerce.inventoryservice.repository.StockReservationRepository;
import java.time.LocalDateTime;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class InventoryServiceApplicationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private InventoryItemRepository inventoryItemRepository;

    @Autowired
    private StockReservationRepository stockReservationRepository;

    @Test
    void contextLoads() {
    }

    @Test
    void createInventoryReturnsSuccessResponse() throws Exception {
        Map<String, Object> request = Map.of(
                "productId", "P1001",
                "productName", "Sample Product",
                "totalQuantity", 25,
                "lowStockThreshold", 5,
                "active", true
        );

        mockMvc.perform(post("/api/inventory")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Inventory created successfully"))
                .andExpect(jsonPath("$.data.productId").value("P1001"))
                .andExpect(jsonPath("$.data.totalQuantity").value(25))
                .andExpect(jsonPath("$.errorCode").value(""));
    }

    @Test
    void restockMissingInventoryCreatesSuccessResponse() throws Exception {
        Map<String, Object> request = Map.of(
                "quantity", 5,
                "note", "new stock arrived"
        );

        mockMvc.perform(post("/api/inventory/P2002/restock")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Inventory restocked successfully"))
                .andExpect(jsonPath("$.data.productId").value("P2002"))
                .andExpect(jsonPath("$.data.totalQuantity").value(5))
                .andExpect(jsonPath("$.data.availableQuantity").value(5))
                .andExpect(jsonPath("$.errorCode").value(""));
    }

    @Test
    void restockViaGetQueryParamsWorks() throws Exception {
        mockMvc.perform(get("/api/inventory/P3003/restock")
                        .param("quantity", "7")
                        .param("note", "browser test"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Inventory restocked successfully"))
                .andExpect(jsonPath("$.data.productId").value("P3003"))
                .andExpect(jsonPath("$.data.totalQuantity").value(7))
                .andExpect(jsonPath("$.data.availableQuantity").value(7))
                .andExpect(jsonPath("$.errorCode").value(""));
    }

    @Test
    void restockWithoutQuantityReturnsValidationError() throws Exception {
        mockMvc.perform(get("/api/inventory/P3003/restock"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message").value("Missing required request parameter: quantity"));
    }

    @Test
    void getOnPostOnlyEndpointReturnsMethodNotAllowed() throws Exception {
        mockMvc.perform(post("/api/inventory/low-stock"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("METHOD_NOT_ALLOWED"));
    }

    @Test
    void getOrderStockReturnsAssignedItemDetails() throws Exception {
        InventoryItem item = new InventoryItem();
        item.setProductId("P4004");
        item.setProductName("Delivery Item");
        item.setTotalQuantity(12);
        item.setAvailableQuantity(7);
        item.setReservedQuantity(5);
        item.setLowStockThreshold(2);
        item.setActive(true);
        inventoryItemRepository.save(item);

        StockReservation reservation = new StockReservation();
        reservation.setOrderId("ORD-9001");
        reservation.setProductId("P4004");
        reservation.setQuantity(5);
        reservation.setStatus(ReservationStatus.RESERVED);
        reservation.setReason("Reserved for delivery");
        reservation.setEventId("event-9001");
        reservation.setCreatedAt(LocalDateTime.now());
        stockReservationRepository.save(reservation);

        mockMvc.perform(get("/api/inventory/order/ORD-9001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Order stock fetched successfully"))
                .andExpect(jsonPath("$.data.orderId").value("ORD-9001"))
                .andExpect(jsonPath("$.data.items[0].productId").value("P4004"))
                .andExpect(jsonPath("$.data.items[0].productName").value("Delivery Item"))
                .andExpect(jsonPath("$.data.items[0].quantity").value(5))
                .andExpect(jsonPath("$.data.items[0].status").value("RESERVED"))
                .andExpect(jsonPath("$.data.items[0].availableQuantity").value(7))
                .andExpect(jsonPath("$.data.items[0].reservedQuantity").value(5))
                .andExpect(jsonPath("$.data.items[0].active").value(true))
                .andExpect(jsonPath("$.errorCode").value(""));
    }

    @Test
    void getOrderStockForMissingOrderReturnsNotFoundResponse() throws Exception {
        mockMvc.perform(get("/api/inventory/order/ORD-NOT-FOUND"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("ORDER_STOCK_NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("Order stock not found for orderId: ORD-NOT-FOUND"));
    }

    @Test
    void faviconReturnsNotFoundResponse() throws Exception {
        mockMvc.perform(get("/favicon.ico"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("RESOURCE_NOT_FOUND"));
    }
}
