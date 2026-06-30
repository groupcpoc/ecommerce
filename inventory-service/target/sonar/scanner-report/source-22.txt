package com.ecommerce.inventoryservice.event;

import java.util.ArrayList;
import java.util.List;

public class PaymentProcessedEvent {

    private String eventId;
    private String orderId;
    private String paymentId;
    private List<PaymentItemEvent> items = new ArrayList<>();

    public String getEventId() {
        return eventId;
    }

    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    public String getPaymentId() {
        return paymentId;
    }

    public void setPaymentId(String paymentId) {
        this.paymentId = paymentId;
    }

    public List<PaymentItemEvent> getItems() {
        return items;
    }

    public void setItems(List<PaymentItemEvent> items) {
        this.items = items;
    }
}
