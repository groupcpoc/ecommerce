package com.ecommerce.inventoryservice;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
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
    void faviconReturnsNotFoundResponse() throws Exception {
        mockMvc.perform(get("/favicon.ico"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("RESOURCE_NOT_FOUND"));
    }
}
