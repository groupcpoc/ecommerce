package com.ecommerce.inventoryservice.exception;

public class OrderStockNotFoundException extends RuntimeException {
    public OrderStockNotFoundException(String message) {
        super(message);
    }
}
