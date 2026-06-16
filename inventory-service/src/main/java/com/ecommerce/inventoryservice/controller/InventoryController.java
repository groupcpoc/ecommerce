package com.ecommerce.inventoryservice.controller;

import java.util.List;

import com.ecommerce.inventoryservice.dto.ApiResponse;
import com.ecommerce.inventoryservice.dto.InventoryCreateRequest;
import com.ecommerce.inventoryservice.dto.InventoryResponse;
import com.ecommerce.inventoryservice.dto.InventorySummaryResponse;
import com.ecommerce.inventoryservice.dto.InventoryUpdateRequest;
import com.ecommerce.inventoryservice.dto.RestockRequest;
import com.ecommerce.inventoryservice.service.InventoryService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/inventory")
@Validated
public class InventoryController {

    private final InventoryService inventoryService;

    public InventoryController(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<InventorySummaryResponse>>> getAllInventory() {
        return ResponseEntity.ok(ApiResponse.success("Inventory fetched successfully", inventoryService.getAllInventory()));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<InventoryResponse>> createInventory(@Valid @RequestBody InventoryCreateRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Inventory created successfully",
                inventoryService.createInventory(request)));
    }

    @GetMapping("/{productId}")
    public ResponseEntity<ApiResponse<InventoryResponse>> getInventoryByProductId(
            @PathVariable @NotBlank String productId) {
        return ResponseEntity.ok(ApiResponse.success("Inventory fetched successfully", inventoryService.getInventory(productId)));
    }

    @PutMapping("/{productId}")
    public ResponseEntity<ApiResponse<InventoryResponse>> updateInventory(
            @PathVariable @NotBlank String productId,
            @Valid @RequestBody InventoryUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Inventory updated successfully",
                inventoryService.updateInventory(productId, request)));
    }

    @PostMapping("/{productId}/restock")
    public ResponseEntity<ApiResponse<InventoryResponse>> restockInventory(
            @PathVariable @NotBlank String productId,
            @Valid @RequestBody RestockRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Inventory restocked successfully",
                inventoryService.restock(productId, request)));
    }

    @GetMapping("/{productId}/restock")
    public ResponseEntity<ApiResponse<InventoryResponse>> restockInventoryFromQuery(
            @PathVariable @NotBlank String productId,
            @RequestParam Integer quantity,
            @RequestParam(required = false) String note) {
        RestockRequest request = new RestockRequest();
        request.setQuantity(quantity);
        request.setNote(note);
        return ResponseEntity.ok(ApiResponse.success("Inventory restocked successfully",
                inventoryService.restock(productId, request)));
    }

    @GetMapping("/low-stock")
    public ResponseEntity<ApiResponse<List<InventorySummaryResponse>>> getLowStockInventory() {
        return ResponseEntity.ok(ApiResponse.success("Low stock inventory fetched successfully",
                inventoryService.getLowStockInventory()));
    }
}
