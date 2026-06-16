package com.ecommerce.inventoryservice.exception;

public class InventoryDomainException extends RuntimeException {

    public InventoryDomainException(String message) {
        super(message);
    }
}
