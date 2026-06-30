package com.ecommerce.inventoryservice.mapper;

import java.util.List;
import java.util.stream.Collectors;

import com.ecommerce.inventoryservice.dto.InventoryResponse;
import com.ecommerce.inventoryservice.dto.InventorySummaryResponse;
import com.ecommerce.inventoryservice.dto.OrderStockItemResponse;
import com.ecommerce.inventoryservice.dto.ReservationResponse;
import com.ecommerce.inventoryservice.entity.InventoryItem;
import com.ecommerce.inventoryservice.entity.StockReservation;
import org.springframework.stereotype.Component;

@Component
public class InventoryMapper {

    public InventoryResponse toResponse(InventoryItem item, List<ReservationResponse> reservations) {
        InventoryResponse response = new InventoryResponse();
        response.setProductId(item.getProductId());
        response.setProductName(item.getProductName());
        response.setTotalQuantity(safeInt(item.getTotalQuantity()));
        response.setAvailableQuantity(safeInt(item.getAvailableQuantity()));
        response.setReservedQuantity(safeInt(item.getReservedQuantity()));
        response.setLowStockThreshold(safeInt(item.getLowStockThreshold()));
        response.setActive(item.getActive());
        response.setLowStock(isLowStock(item));
        response.setUpdatedAt(item.getUpdatedAt());
        response.setReservations(reservations);
        return response;
    }

    public InventoryResponse toResponse(InventoryItem item) {
        return toResponse(item, null);
    }

    public InventorySummaryResponse toSummaryResponse(InventoryItem item) {
        InventorySummaryResponse response = new InventorySummaryResponse();
        response.setProductId(item.getProductId());
        response.setProductName(item.getProductName());
        response.setTotalQuantity(safeInt(item.getTotalQuantity()));
        response.setAvailableQuantity(safeInt(item.getAvailableQuantity()));
        response.setReservedQuantity(safeInt(item.getReservedQuantity()));
        response.setLowStockThreshold(safeInt(item.getLowStockThreshold()));
        response.setActive(item.getActive());
        response.setLowStock(isLowStock(item));
        response.setUpdatedAt(item.getUpdatedAt());
        return response;
    }

    public ReservationResponse toReservationResponse(StockReservation reservation) {
        ReservationResponse response = new ReservationResponse();
        response.setOrderId(reservation.getOrderId());
        response.setProductId(reservation.getProductId());
        response.setQuantity(reservation.getQuantity());
        response.setStatus(reservation.getStatus() != null ? reservation.getStatus().name() : null);
        response.setReason(reservation.getReason());
        response.setEventId(reservation.getEventId());
        response.setCreatedAt(reservation.getCreatedAt());
        return response;
    }

    public List<ReservationResponse> toReservationResponseList(List<StockReservation> reservations) {
        if (reservations == null) {
            return null;
        }
        return reservations.stream()
                .map(this::toReservationResponse)
                .collect(Collectors.toList());
    }

    public OrderStockItemResponse toOrderStockItemResponse(StockReservation reservation, InventoryItem item) {
        OrderStockItemResponse response = new OrderStockItemResponse();
        response.setProductId(reservation.getProductId());
        response.setQuantity(reservation.getQuantity());
        response.setStatus(reservation.getStatus() != null ? reservation.getStatus().name() : null);
        response.setReason(reservation.getReason());
        response.setEventId(reservation.getEventId());
        response.setCreatedAt(reservation.getCreatedAt());

        if (item != null) {
            response.setProductName(item.getProductName());
            response.setTotalQuantity(safeInt(item.getTotalQuantity()));
            response.setAvailableQuantity(safeInt(item.getAvailableQuantity()));
            response.setReservedQuantity(safeInt(item.getReservedQuantity()));
            response.setActive(item.getActive());
        } else {
            response.setProductName(reservation.getProductId());
        }

        return response;
    }

    private boolean isLowStock(InventoryItem item) {
        return safeInt(item.getAvailableQuantity()) <= safeInt(item.getLowStockThreshold());
    }

    private int safeInt(Integer value) {
        return value == null ? 0 : value;
    }
}
