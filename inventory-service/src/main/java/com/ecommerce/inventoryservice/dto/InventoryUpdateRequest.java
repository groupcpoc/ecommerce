package com.ecommerce.inventoryservice.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class InventoryUpdateRequest {

    @NotBlank(message = "productName is required")
    private String productName;

    @NotNull(message = "totalQuantity is required")
    @Min(value = 0, message = "totalQuantity must be greater than or equal to 0")
    private Integer totalQuantity;

    @NotNull(message = "lowStockThreshold is required")
    @Min(value = 0, message = "lowStockThreshold must be greater than or equal to 0")
    private Integer lowStockThreshold;

    @NotNull(message = "active is required")
    private Boolean active;

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public Integer getTotalQuantity() {
        return totalQuantity;
    }

    public void setTotalQuantity(Integer totalQuantity) {
        this.totalQuantity = totalQuantity;
    }

    public Integer getLowStockThreshold() {
        return lowStockThreshold;
    }

    public void setLowStockThreshold(Integer lowStockThreshold) {
        this.lowStockThreshold = lowStockThreshold;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }
}
