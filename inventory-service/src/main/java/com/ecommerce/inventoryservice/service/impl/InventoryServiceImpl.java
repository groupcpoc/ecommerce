package com.ecommerce.inventoryservice.service.impl;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import com.ecommerce.inventoryservice.dto.InventoryCreateRequest;
import com.ecommerce.inventoryservice.dto.InventoryResponse;
import com.ecommerce.inventoryservice.dto.InventorySummaryResponse;
import com.ecommerce.inventoryservice.dto.InventoryUpdateRequest;
import com.ecommerce.inventoryservice.dto.ReservationResponse;
import com.ecommerce.inventoryservice.dto.RestockRequest;
import com.ecommerce.inventoryservice.entity.InventoryItem;
import com.ecommerce.inventoryservice.entity.ProcessedKafkaEvent;
import com.ecommerce.inventoryservice.entity.ReservationStatus;
import com.ecommerce.inventoryservice.entity.StockReservation;
import com.ecommerce.inventoryservice.event.InventoryEvent;
import com.ecommerce.inventoryservice.event.OrderCancelledEvent;
import com.ecommerce.inventoryservice.event.PaymentItemEvent;
import com.ecommerce.inventoryservice.event.PaymentProcessedEvent;
import com.ecommerce.inventoryservice.exception.InventoryDomainException;
import com.ecommerce.inventoryservice.exception.InventoryNotFoundException;
import com.ecommerce.inventoryservice.repository.InventoryItemRepository;
import com.ecommerce.inventoryservice.repository.ProcessedKafkaEventRepository;
import com.ecommerce.inventoryservice.repository.StockReservationRepository;
import com.ecommerce.inventoryservice.service.InventoryService;
import com.ecommerce.inventoryservice.service.KafkaEventPublisher;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InventoryServiceImpl implements InventoryService {

    private final InventoryItemRepository inventoryItemRepository;
    private final StockReservationRepository stockReservationRepository;
    private final ProcessedKafkaEventRepository processedKafkaEventRepository;
    private final KafkaEventPublisher kafkaEventPublisher;
    private final String inventoryReservedTopic;
    private final String inventoryFailedTopic;
    private final String inventoryReleasedTopic;

    public InventoryServiceImpl(InventoryItemRepository inventoryItemRepository,
                                StockReservationRepository stockReservationRepository,
                                ProcessedKafkaEventRepository processedKafkaEventRepository,
                                KafkaEventPublisher kafkaEventPublisher,
                                @Value("${app.kafka.topics.inventory-reserved}") String inventoryReservedTopic,
                                @Value("${app.kafka.topics.inventory-failed}") String inventoryFailedTopic,
                                @Value("${app.kafka.topics.inventory-released}") String inventoryReleasedTopic) {
        this.inventoryItemRepository = inventoryItemRepository;
        this.stockReservationRepository = stockReservationRepository;
        this.processedKafkaEventRepository = processedKafkaEventRepository;
        this.kafkaEventPublisher = kafkaEventPublisher;
        this.inventoryReservedTopic = inventoryReservedTopic;
        this.inventoryFailedTopic = inventoryFailedTopic;
        this.inventoryReleasedTopic = inventoryReleasedTopic;
    }

    @Override
    @Transactional(readOnly = true)
    public List<InventorySummaryResponse> getAllInventory() {
        return inventoryItemRepository.findAllByOrderByProductIdAsc()
                .stream()
                .map(this::toSummaryResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public InventoryResponse getInventory(String productId) {
        InventoryItem item = getInventoryItem(productId);
        InventoryResponse response = toResponse(item);
        response.setReservations(getReservations(productId));
        return response;
    }

    @Override
    @Transactional
    public InventoryResponse createInventory(InventoryCreateRequest request) {
        if (inventoryItemRepository.existsByProductId(request.getProductId())) {
            throw new InventoryDomainException("Inventory already exists for productId: " + request.getProductId());
        }

        InventoryItem item = new InventoryItem();
        item.setProductId(request.getProductId());
        item.setProductName(request.getProductName());
        item.setTotalQuantity(request.getTotalQuantity());
        item.setAvailableQuantity(request.getTotalQuantity());
        item.setReservedQuantity(0);
        item.setLowStockThreshold(request.getLowStockThreshold());
        item.setActive(request.getActive());
        inventoryItemRepository.save(item);
        return toResponse(item);
    }

    @Override
    @Transactional
    public InventoryResponse updateInventory(String productId, InventoryUpdateRequest request) {
        InventoryItem item = inventoryItemRepository.findByProductId(productId).orElseGet(InventoryItem::new);
        int reservedQuantity = item.getReservedQuantity() == null ? 0 : item.getReservedQuantity();
        if (request.getTotalQuantity() < reservedQuantity) {
            throw new InventoryDomainException("totalQuantity cannot be less than reserved quantity");
        }
        item.setProductId(productId);
        item.setProductName(request.getProductName());
        item.setTotalQuantity(request.getTotalQuantity());
        item.setLowStockThreshold(request.getLowStockThreshold());
        item.setActive(request.getActive());
        item.setReservedQuantity(reservedQuantity);
        item.setAvailableQuantity(request.getTotalQuantity() - reservedQuantity);
        inventoryItemRepository.save(item);
        return toResponse(item);
    }

    @Override
    @Transactional
    public InventoryResponse restock(String productId, RestockRequest request) {
        InventoryItem item = inventoryItemRepository.findByProductId(productId)
                .orElseGet(() -> createInventoryShell(productId));
        int currentTotalQuantity = safeInt(item.getTotalQuantity());
        int currentAvailableQuantity = safeInt(item.getAvailableQuantity());
        item.setTotalQuantity(Math.addExact(currentTotalQuantity, request.getQuantity()));
        item.setAvailableQuantity(Math.addExact(currentAvailableQuantity, request.getQuantity()));
        inventoryItemRepository.save(item);
        return toResponse(item);
    }

    @Override
    @Transactional(readOnly = true)
    public List<InventorySummaryResponse> getLowStockInventory() {
        return inventoryItemRepository.findAllByOrderByProductIdAsc()
                .stream()
                .filter(this::isLowStock)
                .map(this::toSummaryResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void processPaymentProcessedEvent(PaymentProcessedEvent event) {
        if (event == null || event.getItems() == null || event.getItems().isEmpty()) {
            publishFailure(null, null, null, "INVALID_PAYMENT_EVENT", "Payment event has no items");
            return;
        }

        String effectiveEventId = nonBlankEventId(event);
        if (processedKafkaEventRepository.findByEventId(effectiveEventId).isPresent()) {
            return;
        }

        List<ReservationPlan> plans = new ArrayList<>();
        for (PaymentItemEvent itemEvent : event.getItems()) {
            InventoryItem item = inventoryItemRepository.findByProductId(itemEvent.getProductId()).orElse(null);
            if (item == null) {
                publishFailure(event, itemEvent.getProductId(), itemEvent.getQuantity(), "PRODUCT_NOT_FOUND", "Product not found");
                return;
            }
            if (itemEvent.getQuantity() == null || itemEvent.getQuantity() <= 0) {
                publishFailure(event, itemEvent.getProductId(), itemEvent.getQuantity(), "INVALID_QUANTITY", "Quantity must be greater than zero");
                return;
            }
            if (safeInt(item.getAvailableQuantity()) < itemEvent.getQuantity()) {
                publishFailure(event, itemEvent.getProductId(), itemEvent.getQuantity(), "OUT_OF_STOCK", "Insufficient stock");
                return;
            }
            plans.add(new ReservationPlan(item, itemEvent));
        }

        List<StockReservation> reservations = new ArrayList<>();
        for (ReservationPlan plan : plans) {
            InventoryItem item = plan.inventoryItem();
            PaymentItemEvent paymentItemEvent = plan.paymentItemEvent();
            int currentAvailable = safeInt(item.getAvailableQuantity());
            int currentReserved = safeInt(item.getReservedQuantity());
            item.setAvailableQuantity(currentAvailable - paymentItemEvent.getQuantity());
            item.setReservedQuantity(currentReserved + paymentItemEvent.getQuantity());
            inventoryItemRepository.save(item);

            StockReservation reservation = new StockReservation();
            reservation.setOrderId(event.getOrderId());
            reservation.setProductId(item.getProductId());
            reservation.setQuantity(paymentItemEvent.getQuantity());
            reservation.setStatus(ReservationStatus.RESERVED);
            reservation.setReason("Stock reserved successfully");
            reservation.setEventId(effectiveEventId);
            reservation.setCreatedAt(LocalDateTime.now());
            stockReservationRepository.save(reservation);
            reservations.add(reservation);
        }

        if (effectiveEventId != null) {
            ProcessedKafkaEvent processedKafkaEvent = new ProcessedKafkaEvent();
            processedKafkaEvent.setEventId(effectiveEventId);
            processedKafkaEvent.setEventType("payment.processed");
            processedKafkaEvent.setProcessedAt(LocalDateTime.now());
            processedKafkaEventRepository.save(processedKafkaEvent);
        }

        publishSuccess(reservations);
    }

    @Override
    @Transactional
    public void releaseInventoryForOrder(OrderCancelledEvent event) {
        if (event == null || event.getOrderId() == null || event.getOrderId().isBlank()) {
            return;
        }

        List<StockReservation> reservations = stockReservationRepository.findByOrderIdAndStatusOrderByCreatedAtDesc(
                event.getOrderId(), ReservationStatus.RESERVED);

        if (reservations.isEmpty()) {
            return;
        }

        for (StockReservation reservation : reservations) {
            InventoryItem item = inventoryItemRepository.findByProductId(reservation.getProductId()).orElse(null);
            if (item == null) {
                continue;
            }
            int reservationQuantity = safeInt(reservation.getQuantity());
            int currentReserved = safeInt(item.getReservedQuantity());
            int currentAvailable = safeInt(item.getAvailableQuantity());
            item.setReservedQuantity(Math.max(currentReserved - reservationQuantity, 0));
            item.setAvailableQuantity(Math.addExact(currentAvailable, reservationQuantity));
            inventoryItemRepository.save(item);

            reservation.setStatus(ReservationStatus.RELEASED);
            reservation.setReason(event.getReason() != null ? event.getReason() : "Order cancelled");
            stockReservationRepository.save(reservation);

            InventoryEvent inventoryEvent = new InventoryEvent();
            inventoryEvent.setEventId(event.getEventId() != null ? event.getEventId() : nonBlankEventIdFromOrder(event.getOrderId()));
            inventoryEvent.setEventType("inventory.released");
            inventoryEvent.setOrderId(event.getOrderId());
            inventoryEvent.setProductId(item.getProductId());
            inventoryEvent.setQuantity(reservation.getQuantity());
            inventoryEvent.setReason(reservation.getReason());
            inventoryEvent.setCreatedAt(LocalDateTime.now());
            kafkaEventPublisher.publish(inventoryReleasedTopic, inventoryEvent);
        }
    }

    private void publishSuccess(List<StockReservation> reservations) {
        for (StockReservation reservation : reservations) {
            InventoryEvent inventoryEvent = new InventoryEvent();
            inventoryEvent.setEventId(reservation.getEventId());
            inventoryEvent.setEventType("inventory.reserved");
            inventoryEvent.setOrderId(reservation.getOrderId());
            inventoryEvent.setProductId(reservation.getProductId());
            inventoryEvent.setQuantity(reservation.getQuantity());
            inventoryEvent.setReason("Stock successfully reserved for an order");
            inventoryEvent.setCreatedAt(LocalDateTime.now());
            kafkaEventPublisher.publish(inventoryReservedTopic, inventoryEvent);
        }
    }

    private void publishFailure(PaymentProcessedEvent event, String productId, Integer quantity, String reasonCode, String message) {
        InventoryEvent inventoryEvent = new InventoryEvent();
        inventoryEvent.setEventId(event != null ? nonBlankEventId(event) : "inventory-failed-" + System.currentTimeMillis());
        inventoryEvent.setEventType("inventory.failed");
        inventoryEvent.setOrderId(event != null ? event.getOrderId() : null);
        inventoryEvent.setProductId(productId);
        inventoryEvent.setQuantity(quantity);
        inventoryEvent.setReason(reasonCode + ": " + message);
        inventoryEvent.setCreatedAt(LocalDateTime.now());
        kafkaEventPublisher.publish(inventoryFailedTopic, inventoryEvent);
    }

    private InventoryItem getInventoryItem(String productId) {
        return inventoryItemRepository.findByProductId(productId)
                .orElseThrow(() -> new InventoryNotFoundException("Inventory not found for productId: " + productId));
    }

    private InventoryItem createInventoryShell(String productId) {
        InventoryItem item = new InventoryItem();
        item.setProductId(productId);
        item.setProductName(productId);
        item.setTotalQuantity(0);
        item.setAvailableQuantity(0);
        item.setReservedQuantity(0);
        item.setLowStockThreshold(0);
        item.setActive(true);
        return item;
    }

    private InventoryResponse toResponse(InventoryItem item) {
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
        return response;
    }

    private InventorySummaryResponse toSummaryResponse(InventoryItem item) {
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

    private boolean isLowStock(InventoryItem item) {
        return safeInt(item.getAvailableQuantity()) <= safeInt(item.getLowStockThreshold());
    }

    private int safeInt(Integer value) {
        return value == null ? 0 : value;
    }

    private List<ReservationResponse> getReservations(String productId) {
        return stockReservationRepository.findByProductIdOrderByCreatedAtDesc(productId)
                .stream()
                .map(reservation -> {
                    ReservationResponse response = new ReservationResponse();
                    response.setOrderId(reservation.getOrderId());
                    response.setProductId(reservation.getProductId());
                    response.setQuantity(reservation.getQuantity());
                    response.setStatus(reservation.getStatus() != null ? reservation.getStatus().name() : null);
                    response.setReason(reservation.getReason());
                    response.setEventId(reservation.getEventId());
                    response.setCreatedAt(reservation.getCreatedAt());
                    return response;
                })
                .collect(Collectors.toList());
    }

    private String nonBlankEventId(PaymentProcessedEvent event) {
        return event.getEventId() == null || event.getEventId().isBlank()
                ? "payment-" + System.currentTimeMillis()
                : event.getEventId();
    }

    private String nonBlankEventIdFromOrder(String orderId) {
        return orderId == null || orderId.isBlank()
                ? "order-" + System.currentTimeMillis()
                : orderId;
    }

    private record ReservationPlan(InventoryItem inventoryItem, PaymentItemEvent paymentItemEvent) {
    }
}
