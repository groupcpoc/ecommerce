package com.ecommerce.inventoryservice.dto;

import java.util.List;

public class OrderStockResponse {
    private String orderId;
    private List<OrderStockItemResponse> items;

    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    public List<OrderStockItemResponse> getItems() {
        return items;
    }

    public void setItems(List<OrderStockItemResponse> items) {
        this.items = items;
    }
}
