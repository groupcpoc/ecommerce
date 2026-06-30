package com.ecommerce.inventoryservice;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import com.ecommerce.inventoryservice.config.KafkaConfig;
import com.ecommerce.inventoryservice.consumer.KafkaInventoryConsumer;
import com.ecommerce.inventoryservice.controller.InventoryController;
import com.ecommerce.inventoryservice.dto.*;
import com.ecommerce.inventoryservice.entity.*;
import com.ecommerce.inventoryservice.event.*;
import com.ecommerce.inventoryservice.exception.*;
import com.ecommerce.inventoryservice.mapper.InventoryMapper;
import com.ecommerce.inventoryservice.publisher.KafkaEventPublisherImpl;
import com.ecommerce.inventoryservice.publisher.NoOpKafkaEventPublisher;
import com.ecommerce.inventoryservice.repository.InventoryItemRepository;
import com.ecommerce.inventoryservice.repository.ProcessedKafkaEventRepository;
import com.ecommerce.inventoryservice.repository.StockReservationRepository;
import com.ecommerce.inventoryservice.service.InventoryService;
import com.ecommerce.inventoryservice.service.impl.InventoryServiceImpl;
import com.ecommerce.inventoryservice.util.InventoryUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.boot.ssl.SslBundles;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.SendResult;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@ExtendWith(MockitoExtension.class)
class UnitTestSuiteTest {

    @Mock
    private InventoryItemRepository inventoryItemRepository;

    @Mock
    private StockReservationRepository stockReservationRepository;

    @Mock
    private ProcessedKafkaEventRepository processedKafkaEventRepository;

    @Mock
    private com.ecommerce.inventoryservice.publisher.KafkaEventPublisher kafkaEventPublisher;

    @InjectMocks
    private InventoryServiceImpl inventoryService;

    // ---------------------------------------------------------
    // 1. InventoryServiceImpl Tests
    // ---------------------------------------------------------

    @Test
    void testProcessPaymentProcessedEvent_NullEvent() {
        InventoryServiceImpl service = new InventoryServiceImpl(
                inventoryItemRepository, stockReservationRepository, processedKafkaEventRepository,
                kafkaEventPublisher, new InventoryMapper(), "res", "fail", "rel"
        );
        service.processPaymentProcessedEvent(null);
        verify(kafkaEventPublisher).publish(eq("fail"), any());
    }

    @Test
    void testProcessPaymentProcessedEvent_EmptyItems() {
        InventoryServiceImpl service = new InventoryServiceImpl(
                inventoryItemRepository, stockReservationRepository, processedKafkaEventRepository,
                kafkaEventPublisher, new InventoryMapper(), "res", "fail", "rel"
        );
        PaymentProcessedEvent event = new PaymentProcessedEvent();
        event.setOrderId("O1");
        event.setItems(Collections.emptyList());
        service.processPaymentProcessedEvent(event);
        verify(kafkaEventPublisher).publish(eq("fail"), any());
    }

    @Test
    void testProcessPaymentProcessedEvent_AlreadyProcessed() {
        InventoryServiceImpl service = new InventoryServiceImpl(
                inventoryItemRepository, stockReservationRepository, processedKafkaEventRepository,
                kafkaEventPublisher, new InventoryMapper(), "res", "fail", "rel"
        );
        PaymentProcessedEvent event = new PaymentProcessedEvent();
        event.setEventId("E1");
        event.setOrderId("O1");
        PaymentItemEvent item = new PaymentItemEvent();
        item.setProductId("P1");
        item.setQuantity(2);
        event.setItems(List.of(item));

        ProcessedKafkaEvent dbEvent = new ProcessedKafkaEvent();
        dbEvent.setEventId("E1");
        when(processedKafkaEventRepository.findByEventId("E1")).thenReturn(Optional.of(dbEvent));

        service.processPaymentProcessedEvent(event);
        verifyNoInteractions(inventoryItemRepository);
    }

    @Test
    void testProcessPaymentProcessedEvent_ProductNotFound() {
        InventoryServiceImpl service = new InventoryServiceImpl(
                inventoryItemRepository, stockReservationRepository, processedKafkaEventRepository,
                kafkaEventPublisher, new InventoryMapper(), "res", "fail", "rel"
        );
        PaymentProcessedEvent event = new PaymentProcessedEvent();
        event.setOrderId("O1");
        PaymentItemEvent item = new PaymentItemEvent();
        item.setProductId("P1");
        item.setQuantity(2);
        event.setItems(List.of(item));

        when(processedKafkaEventRepository.findByEventId(any())).thenReturn(Optional.empty());
        when(inventoryItemRepository.findByProductId("P1")).thenReturn(Optional.empty());

        service.processPaymentProcessedEvent(event);
        verify(kafkaEventPublisher).publish(eq("fail"), any());
    }

    @Test
    void testProcessPaymentProcessedEvent_InvalidQuantity() {
        InventoryServiceImpl service = new InventoryServiceImpl(
                inventoryItemRepository, stockReservationRepository, processedKafkaEventRepository,
                kafkaEventPublisher, new InventoryMapper(), "res", "fail", "rel"
        );
        PaymentProcessedEvent event = new PaymentProcessedEvent();
        event.setOrderId("O1");
        PaymentItemEvent item = new PaymentItemEvent();
        item.setProductId("P1");
        item.setQuantity(0); // <= 0
        event.setItems(List.of(item));

        InventoryItem inv = new InventoryItem();
        inv.setProductId("P1");

        when(processedKafkaEventRepository.findByEventId(any())).thenReturn(Optional.empty());
        when(inventoryItemRepository.findByProductId("P1")).thenReturn(Optional.of(inv));

        service.processPaymentProcessedEvent(event);
        verify(kafkaEventPublisher).publish(eq("fail"), any());
    }

    @Test
    void testProcessPaymentProcessedEvent_InsufficientStock() {
        InventoryServiceImpl service = new InventoryServiceImpl(
                inventoryItemRepository, stockReservationRepository, processedKafkaEventRepository,
                kafkaEventPublisher, new InventoryMapper(), "res", "fail", "rel"
        );
        PaymentProcessedEvent event = new PaymentProcessedEvent();
        event.setOrderId("O1");
        PaymentItemEvent item = new PaymentItemEvent();
        item.setProductId("P1");
        item.setQuantity(5);
        event.setItems(List.of(item));

        InventoryItem inv = new InventoryItem();
        inv.setProductId("P1");
        inv.setAvailableQuantity(2); // Less than 5

        when(processedKafkaEventRepository.findByEventId(any())).thenReturn(Optional.empty());
        when(inventoryItemRepository.findByProductId("P1")).thenReturn(Optional.of(inv));

        service.processPaymentProcessedEvent(event);
        verify(kafkaEventPublisher).publish(eq("fail"), any());
    }

    @Test
    void testReleaseInventoryForOrder_NullOrderId() {
        InventoryServiceImpl service = new InventoryServiceImpl(
                inventoryItemRepository, stockReservationRepository, processedKafkaEventRepository,
                kafkaEventPublisher, new InventoryMapper(), "res", "fail", "rel"
        );
        service.releaseInventoryForOrder(null);
        verifyNoInteractions(stockReservationRepository);
    }

    @Test
    void testReleaseInventoryForOrder_NoReservations() {
        InventoryServiceImpl service = new InventoryServiceImpl(
                inventoryItemRepository, stockReservationRepository, processedKafkaEventRepository,
                kafkaEventPublisher, new InventoryMapper(), "res", "fail", "rel"
        );
        OrderCancelledEvent event = new OrderCancelledEvent();
        event.setOrderId("O1");
        when(stockReservationRepository.findByOrderIdAndStatusOrderByCreatedAtDesc("O1", ReservationStatus.RESERVED))
                .thenReturn(Collections.emptyList());

        service.releaseInventoryForOrder(event);
        verifyNoMoreInteractions(inventoryItemRepository);
    }

    @Test
    void testReleaseInventoryForOrder_ProductNotFoundForReservation() {
        InventoryServiceImpl service = new InventoryServiceImpl(
                inventoryItemRepository, stockReservationRepository, processedKafkaEventRepository,
                kafkaEventPublisher, new InventoryMapper(), "res", "fail", "rel"
        );
        OrderCancelledEvent event = new OrderCancelledEvent();
        event.setOrderId("O1");

        StockReservation res = new StockReservation();
        res.setProductId("P1");
        res.setQuantity(2);

        when(stockReservationRepository.findByOrderIdAndStatusOrderByCreatedAtDesc("O1", ReservationStatus.RESERVED))
                .thenReturn(List.of(res));
        when(inventoryItemRepository.findByProductId("P1")).thenReturn(Optional.empty());

        service.releaseInventoryForOrder(event);
        verify(inventoryItemRepository, never()).save(any());
    }

    // ---------------------------------------------------------
    // 2. InventoryMapper Tests
    // ---------------------------------------------------------
    @Test
    void testInventoryMapper() {
        InventoryMapper mapper = new InventoryMapper();

        InventoryItem item = new InventoryItem();
        item.setProductId("P1");
        item.setProductName("Name");
        item.setTotalQuantity(10);
        item.setAvailableQuantity(8);
        item.setReservedQuantity(2);
        item.setLowStockThreshold(3);
        item.setActive(true);
        item.setUpdatedAt(LocalDateTime.now());

        assertNotNull(mapper.toResponse(item));
        assertNotNull(mapper.toResponse(item, Collections.emptyList()));
        assertNotNull(mapper.toSummaryResponse(item));

        StockReservation res = new StockReservation();
        res.setOrderId("O1");
        res.setProductId("P1");
        res.setQuantity(2);
        res.setStatus(ReservationStatus.RESERVED);
        res.setReason("reason");
        res.setEventId("E1");
        res.setCreatedAt(LocalDateTime.now());

        assertNotNull(mapper.toReservationResponse(res));
        assertNotNull(mapper.toReservationResponseList(List.of(res)));
        assertNull(mapper.toReservationResponseList(null));

        assertNotNull(mapper.toOrderStockItemResponse(res, item));
        assertNotNull(mapper.toOrderStockItemResponse(res, null));
    }

    // ---------------------------------------------------------
    // 3. InventoryUtils Tests
    // ---------------------------------------------------------
    @Test
    void testInventoryUtils() {
        LocalDateTime time = LocalDateTime.of(2026, 6, 30, 12, 0, 0);
        assertEquals("2026-06-30 12:00:00", InventoryUtils.formatDateTime(time));
        assertEquals("", InventoryUtils.formatDateTime(null));
        assertEquals(5, InventoryUtils.safeInt(5));
        assertEquals(0, InventoryUtils.safeInt(null));
    }

    // ---------------------------------------------------------
    // 4. KafkaInventoryConsumer Tests
    // ---------------------------------------------------------
    @Test
    void testKafkaInventoryConsumer() throws Exception {
        InventoryServiceImpl mockService = mock(InventoryServiceImpl.class);
        ObjectMapper mapper = new ObjectMapper();
        KafkaInventoryConsumer consumer = new KafkaInventoryConsumer(mockService, mapper);

        consumer.consumePaymentProcessed("{\"orderId\":\"O1\",\"items\":[]}");
        verify(mockService).processPaymentProcessedEvent(any());

        consumer.consumeOrderCancelled("{\"orderId\":\"O1\",\"reason\":\"cancel\"}");
        verify(mockService).releaseInventoryForOrder(any());
    }

    // ---------------------------------------------------------
    // 5. KafkaEventPublisherImpl Tests
    // ---------------------------------------------------------
    @SuppressWarnings("unchecked")
    @Test
    void testKafkaEventPublisherImpl_success() throws JsonProcessingException {
        KafkaTemplate<String, String> mockTemplate = mock(KafkaTemplate.class);
        ObjectMapper mockMapper = mock(ObjectMapper.class);
        KafkaEventPublisherImpl publisher = new KafkaEventPublisherImpl(mockTemplate, mockMapper);

        InventoryEvent event = new InventoryEvent();
        event.setEventId("E1");

        when(mockMapper.writeValueAsString(event)).thenReturn("{}");
        CompletableFuture<SendResult<String, String>> future = CompletableFuture.completedFuture(mock(SendResult.class));
        when(mockTemplate.send("topic", "E1", "{}")).thenReturn(future);

        publisher.publish("topic", event);
        verify(mockTemplate).send("topic", "E1", "{}");
    }

    @SuppressWarnings("unchecked")
    @Test
    void testKafkaEventPublisherImpl_serializationException() throws JsonProcessingException {
        KafkaTemplate<String, String> mockTemplate = mock(KafkaTemplate.class);
        ObjectMapper mockMapper = mock(ObjectMapper.class);
        KafkaEventPublisherImpl publisher = new KafkaEventPublisherImpl(mockTemplate, mockMapper);

        InventoryEvent event = new InventoryEvent();
        event.setEventId("E1");

        when(mockMapper.writeValueAsString(event)).thenThrow(new JsonProcessingException("error") {});
        assertDoesNotThrow(() -> publisher.publish("topic", event));
    }

    @SuppressWarnings("unchecked")
    @Test
    void testKafkaEventPublisherImpl_runtimeException() throws JsonProcessingException {
        KafkaTemplate<String, String> mockTemplate = mock(KafkaTemplate.class);
        ObjectMapper mockMapper = mock(ObjectMapper.class);
        KafkaEventPublisherImpl publisher = new KafkaEventPublisherImpl(mockTemplate, mockMapper);

        InventoryEvent event = new InventoryEvent();
        event.setEventId("E1");

        when(mockMapper.writeValueAsString(event)).thenThrow(new RuntimeException("runtime"));
        assertDoesNotThrow(() -> publisher.publish("topic", event));
    }

    @SuppressWarnings("unchecked")
    @Test
    void testKafkaEventPublisherImpl_callbackFailure() throws JsonProcessingException {
        KafkaTemplate<String, String> mockTemplate = mock(KafkaTemplate.class);
        ObjectMapper mockMapper = mock(ObjectMapper.class);
        KafkaEventPublisherImpl publisher = new KafkaEventPublisherImpl(mockTemplate, mockMapper);

        InventoryEvent event = new InventoryEvent();
        event.setEventId("E1");

        when(mockMapper.writeValueAsString(event)).thenReturn("{}");
        CompletableFuture<SendResult<String, String>> failedFuture = new CompletableFuture<>();
        failedFuture.completeExceptionally(new RuntimeException("kafka down"));
        when(mockTemplate.send("topic", "E1", "{}")).thenReturn(failedFuture);

        assertDoesNotThrow(() -> publisher.publish("topic", event));
    }

    @Test
    void testNoOpKafkaEventPublisher() {
        NoOpKafkaEventPublisher noOp = new NoOpKafkaEventPublisher();
        assertDoesNotThrow(() -> noOp.publish("topic", new InventoryEvent()));
    }

    // ---------------------------------------------------------
    // 6. DTOs and Entities Coverage (Getter / Setter / Constructor)
    // ---------------------------------------------------------
    @Test
    void testDtoAndEntitiesCoverage() {
        ApiResponse<String> apiResponse = new ApiResponse<>(true, "msg", "data", LocalDateTime.now(), "err");
        apiResponse.setSuccess(false);
        apiResponse.setMessage("m");
        apiResponse.setData("d");
        apiResponse.setErrorCode("e");
        assertFalse(apiResponse.isSuccess());
        assertEquals("m", apiResponse.getMessage());
        assertEquals("d", apiResponse.getData());
        assertEquals("e", apiResponse.getErrorCode());

        ApiResponse<Object> api2 = ApiResponse.success("msg", null);
        assertTrue(api2.isSuccess());
        ApiResponse<Object> api3 = ApiResponse.success("msg", "dat");
        assertEquals("dat", api3.getData());

        InventoryCreateRequest createReq = new InventoryCreateRequest();
        createReq.setProductId("P1");
        createReq.setProductName("N1");
        createReq.setTotalQuantity(10);
        createReq.setLowStockThreshold(2);
        createReq.setActive(true);
        assertEquals("P1", createReq.getProductId());
        assertEquals("N1", createReq.getProductName());
        assertEquals(10, createReq.getTotalQuantity());
        assertEquals(2, createReq.getLowStockThreshold());
        assertTrue(createReq.getActive());

        InventoryResponse response = new InventoryResponse();
        response.setProductId("P1");
        response.setProductName("N1");
        response.setTotalQuantity(10);
        response.setAvailableQuantity(8);
        response.setReservedQuantity(2);
        response.setLowStockThreshold(2);
        response.setActive(true);
        response.setLowStock(false);
        LocalDateTime now = LocalDateTime.now();
        response.setUpdatedAt(now);
        response.setReservations(Collections.emptyList());
        assertEquals("P1", response.getProductId());
        assertEquals("N1", response.getProductName());
        assertEquals(10, response.getTotalQuantity());
        assertEquals(8, response.getAvailableQuantity());
        assertEquals(2, response.getReservedQuantity());
        assertEquals(2, response.getLowStockThreshold());
        assertTrue(response.getActive());
        assertFalse(response.getLowStock());
        assertEquals(now, response.getUpdatedAt());
        assertEquals(0, response.getReservations().size());

        InventorySummaryResponse summary = new InventorySummaryResponse();
        summary.setProductId("P1");
        summary.setProductName("N1");
        summary.setTotalQuantity(10);
        summary.setAvailableQuantity(8);
        summary.setReservedQuantity(2);
        summary.setLowStockThreshold(2);
        summary.setActive(true);
        summary.setLowStock(false);
        summary.setUpdatedAt(now);
        assertEquals("P1", summary.getProductId());
        assertEquals("N1", summary.getProductName());
        assertEquals(10, summary.getTotalQuantity());
        assertEquals(8, summary.getAvailableQuantity());
        assertEquals(2, summary.getReservedQuantity());
        assertEquals(2, summary.getLowStockThreshold());
        assertTrue(summary.getActive());
        assertFalse(summary.getLowStock());
        assertEquals(now, summary.getUpdatedAt());

        InventoryUpdateRequest update = new InventoryUpdateRequest();
        update.setProductName("N");
        update.setTotalQuantity(5);
        update.setLowStockThreshold(1);
        update.setActive(false);
        assertEquals("N", update.getProductName());
        assertEquals(5, update.getTotalQuantity());
        assertEquals(1, update.getLowStockThreshold());
        assertFalse(update.getActive());

        OrderStockResponse osr = new OrderStockResponse();
        osr.setOrderId("O1");
        osr.setItems(Collections.emptyList());
        assertEquals("O1", osr.getOrderId());
        assertEquals(0, osr.getItems().size());

        OrderStockItemResponse osir = new OrderStockItemResponse();
        osir.setProductId("P");
        osir.setQuantity(1);
        osir.setStatus("ST");
        osir.setReason("R");
        osir.setEventId("E");
        osir.setCreatedAt(now);
        osir.setProductName("PN");
        osir.setTotalQuantity(5);
        osir.setAvailableQuantity(4);
        osir.setReservedQuantity(1);
        osir.setActive(true);
        assertEquals("P", osir.getProductId());
        assertEquals(1, osir.getQuantity());
        assertEquals("ST", osir.getStatus());
        assertEquals("R", osir.getReason());
        assertEquals("E", osir.getEventId());
        assertEquals(now, osir.getCreatedAt());
        assertEquals("PN", osir.getProductName());
        assertEquals(5, osir.getTotalQuantity());
        assertEquals(4, osir.getAvailableQuantity());
        assertEquals(1, osir.getReservedQuantity());
        assertTrue(osir.getActive());

        ReservationResponse rr = new ReservationResponse();
        rr.setOrderId("O");
        rr.setProductId("P");
        rr.setQuantity(2);
        rr.setStatus("S");
        rr.setReason("R");
        rr.setEventId("E");
        rr.setCreatedAt(now);
        assertEquals("O", rr.getOrderId());
        assertEquals("P", rr.getProductId());
        assertEquals(2, rr.getQuantity());
        assertEquals("S", rr.getStatus());
        assertEquals("R", rr.getReason());
        assertEquals("E", rr.getEventId());
        assertEquals(now, rr.getCreatedAt());

        RestockRequest restock = new RestockRequest();
        restock.setQuantity(10);
        restock.setNote("note");
        assertEquals(10, restock.getQuantity());
        assertEquals("note", restock.getNote());

        ProcessedKafkaEvent pke = new ProcessedKafkaEvent();
        pke.setId(10L);
        pke.setEventId("E");
        pke.setEventType("T");
        pke.setProcessedAt(now);
        assertEquals(10L, pke.getId());
        assertEquals("E", pke.getEventId());
        assertEquals("T", pke.getEventType());
        assertEquals(now, pke.getProcessedAt());

        StockReservation sr = new StockReservation();
        sr.setId(20L);
        sr.setOrderId("O");
        sr.setProductId("P");
        sr.setQuantity(3);
        sr.setStatus(ReservationStatus.RELEASED);
        sr.setReason("reason");
        sr.setEventId("E");
        sr.setCreatedAt(now);
        assertEquals(20L, sr.getId());
        assertEquals("O", sr.getOrderId());
        assertEquals("P", sr.getProductId());
        assertEquals(3, sr.getQuantity());
        assertEquals(ReservationStatus.RELEASED, sr.getStatus());
        assertEquals("reason", sr.getReason());
        assertEquals("E", sr.getEventId());
        assertEquals(now, sr.getCreatedAt());

        InventoryItem entity = new InventoryItem();
        entity.setId(30L);
        entity.setCreatedAt(now);
        entity.setVersion(1L);
        assertEquals(30L, entity.getId());
        assertEquals(now, entity.getCreatedAt());
        assertEquals(1L, entity.getVersion());

        entity.prePersist();
        assertNotNull(entity.getCreatedAt());
        assertNotNull(entity.getUpdatedAt());
        assertEquals(0, entity.getReservedQuantity());

        entity.preUpdate();
        assertNotNull(entity.getUpdatedAt());

        assertEquals(ReservationStatus.RESERVED, ReservationStatus.valueOf("RESERVED"));
    }

    // ---------------------------------------------------------
    // 7. Exception Handler Coverage
    // ---------------------------------------------------------
    @Test
    void testGlobalExceptionHandler() throws Exception {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();

        ResponseEntity<ApiResponse<Object>> resp1 = handler.handleNotFound(new InventoryNotFoundException("not found"));
        assertEquals("INVENTORY_NOT_FOUND", resp1.getBody().getErrorCode());

        ResponseEntity<ApiResponse<Object>> resp2 = handler.handleDomain(new InventoryDomainException("domain"));
        assertEquals("INVENTORY_OPERATION_FAILED", resp2.getBody().getErrorCode());

        ResponseEntity<ApiResponse<Object>> resp3 = handler.handleOrderStockNotFound(new OrderStockNotFoundException("order not found"));
        assertEquals("ORDER_STOCK_NOT_FOUND", resp3.getBody().getErrorCode());

        MethodParameter parameter = new MethodParameter(this.getClass().getDeclaredMethod("dummyMethod", String.class), 0);
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult("target", "targetName");
        bindingResult.addError(new FieldError("target", "field", "default message"));
        MethodArgumentNotValidException valEx = new MethodArgumentNotValidException(parameter, bindingResult);
        ResponseEntity<ApiResponse<Object>> resp4 = handler.handleValidation(valEx);
        assertEquals("VALIDATION_ERROR", resp4.getBody().getErrorCode());

        ConstraintViolationException constraintEx = new ConstraintViolationException("msg", Collections.emptySet());
        ResponseEntity<ApiResponse<Object>> resp5 = handler.handleConstraintViolation(constraintEx);
        assertEquals("VALIDATION_ERROR", resp5.getBody().getErrorCode());

        HttpMessageNotReadableException badReqEx = new HttpMessageNotReadableException("msg");
        ResponseEntity<ApiResponse<Object>> resp6 = handler.handleBadRequest(badReqEx);
        assertEquals("VALIDATION_ERROR", resp6.getBody().getErrorCode());

        MissingServletRequestParameterException paramEx = new MissingServletRequestParameterException("name", "type");
        ResponseEntity<ApiResponse<Object>> resp7 = handler.handleMissingParameter(paramEx);
        assertEquals("VALIDATION_ERROR", resp7.getBody().getErrorCode());

        HttpRequestMethodNotSupportedException methodEx = new HttpRequestMethodNotSupportedException("GET");
        ResponseEntity<ApiResponse<Object>> resp8 = handler.handleMethodNotSupported(methodEx);
        assertEquals(HttpStatus.METHOD_NOT_ALLOWED, resp8.getStatusCode());

        NoResourceFoundException resourceEx = new NoResourceFoundException(null, "path");
        ResponseEntity<ApiResponse<Object>> resp9 = handler.handleNoResourceFound(resourceEx);
        assertEquals(HttpStatus.NOT_FOUND, resp9.getStatusCode());

        ResponseEntity<ApiResponse<Object>> resp10 = handler.handleUnexpected(new Exception("unexpected"));
        assertEquals("UNEXPECTED_ERROR", resp10.getBody().getErrorCode());
        
        ResponseEntity<ApiResponse<Object>> resp11 = handler.handleUnexpected(new Exception(""));
        assertEquals("UNEXPECTED_ERROR", resp11.getBody().getErrorCode());

        ResponseEntity<ApiResponse<Object>> resp12 = handler.handleUnexpected(new Exception((String) null));
        assertEquals("UNEXPECTED_ERROR", resp12.getBody().getErrorCode());
    }

    // ---------------------------------------------------------
    // 8. InventoryServiceImpl – Remaining Methods
    // ---------------------------------------------------------
    @Test
    void testGetAllInventory() {
        InventoryServiceImpl service = new InventoryServiceImpl(
                inventoryItemRepository, stockReservationRepository, processedKafkaEventRepository,
                kafkaEventPublisher, new InventoryMapper(), "res", "fail", "rel"
        );
        InventoryItem item = new InventoryItem();
        item.setProductId("P1");
        item.setProductName("Widget");
        item.setTotalQuantity(10);
        item.setAvailableQuantity(8);
        item.setReservedQuantity(2);
        item.setLowStockThreshold(3);
        item.setActive(true);
        item.setUpdatedAt(LocalDateTime.now());
        when(inventoryItemRepository.findAllByOrderByProductIdAsc()).thenReturn(List.of(item));
        List<InventorySummaryResponse> result = service.getAllInventory();
        assertEquals(1, result.size());
        assertEquals("P1", result.get(0).getProductId());
    }

    @Test
    void testGetInventory() {
        InventoryServiceImpl service = new InventoryServiceImpl(
                inventoryItemRepository, stockReservationRepository, processedKafkaEventRepository,
                kafkaEventPublisher, new InventoryMapper(), "res", "fail", "rel"
        );
        InventoryItem item = new InventoryItem();
        item.setProductId("P1");
        item.setProductName("Widget");
        item.setTotalQuantity(10);
        item.setAvailableQuantity(8);
        item.setReservedQuantity(2);
        item.setLowStockThreshold(3);
        item.setActive(true);
        item.setUpdatedAt(LocalDateTime.now());
        when(inventoryItemRepository.findByProductId("P1")).thenReturn(Optional.of(item));
        when(stockReservationRepository.findByProductIdOrderByCreatedAtDesc("P1")).thenReturn(Collections.emptyList());
        InventoryResponse result = service.getInventory("P1");
        assertEquals("P1", result.getProductId());
    }

    @Test
    void testGetInventory_NotFound() {
        InventoryServiceImpl service = new InventoryServiceImpl(
                inventoryItemRepository, stockReservationRepository, processedKafkaEventRepository,
                kafkaEventPublisher, new InventoryMapper(), "res", "fail", "rel"
        );
        when(inventoryItemRepository.findByProductId("P1")).thenReturn(Optional.empty());
        assertThrows(InventoryNotFoundException.class, () -> service.getInventory("P1"));
    }

    @Test
    void testCreateInventory() {
        InventoryServiceImpl service = new InventoryServiceImpl(
                inventoryItemRepository, stockReservationRepository, processedKafkaEventRepository,
                kafkaEventPublisher, new InventoryMapper(), "res", "fail", "rel"
        );
        InventoryCreateRequest request = new InventoryCreateRequest();
        request.setProductId("P1");
        request.setProductName("Widget");
        request.setTotalQuantity(10);
        request.setLowStockThreshold(2);
        request.setActive(true);
        when(inventoryItemRepository.existsByProductId("P1")).thenReturn(false);
        when(inventoryItemRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        InventoryResponse result = service.createInventory(request);
        assertEquals("P1", result.getProductId());
        assertEquals(10, result.getTotalQuantity());
    }

    @Test
    void testCreateInventory_AlreadyExists() {
        InventoryServiceImpl service = new InventoryServiceImpl(
                inventoryItemRepository, stockReservationRepository, processedKafkaEventRepository,
                kafkaEventPublisher, new InventoryMapper(), "res", "fail", "rel"
        );
        InventoryCreateRequest request = new InventoryCreateRequest();
        request.setProductId("P1");
        when(inventoryItemRepository.existsByProductId("P1")).thenReturn(true);
        assertThrows(InventoryDomainException.class, () -> service.createInventory(request));
    }

    @Test
    void testUpdateInventory_ExistingItem() {
        InventoryServiceImpl service = new InventoryServiceImpl(
                inventoryItemRepository, stockReservationRepository, processedKafkaEventRepository,
                kafkaEventPublisher, new InventoryMapper(), "res", "fail", "rel"
        );
        InventoryItem existing = new InventoryItem();
        existing.setProductId("P1");
        existing.setReservedQuantity(2);
        when(inventoryItemRepository.findByProductId("P1")).thenReturn(Optional.of(existing));
        when(inventoryItemRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        InventoryUpdateRequest request = new InventoryUpdateRequest();
        request.setProductName("Updated");
        request.setTotalQuantity(15);
        request.setLowStockThreshold(3);
        request.setActive(true);

        InventoryResponse result = service.updateInventory("P1", request);
        assertEquals("P1", result.getProductId());
        assertEquals(15, result.getTotalQuantity());
        assertEquals(13, result.getAvailableQuantity());
    }

    @Test
    void testUpdateInventory_NewItem() {
        InventoryServiceImpl service = new InventoryServiceImpl(
                inventoryItemRepository, stockReservationRepository, processedKafkaEventRepository,
                kafkaEventPublisher, new InventoryMapper(), "res", "fail", "rel"
        );
        when(inventoryItemRepository.findByProductId("P1")).thenReturn(Optional.empty());
        when(inventoryItemRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        InventoryUpdateRequest request = new InventoryUpdateRequest();
        request.setProductName("New");
        request.setTotalQuantity(10);
        request.setLowStockThreshold(2);
        request.setActive(true);

        InventoryResponse result = service.updateInventory("P1", request);
        assertEquals("P1", result.getProductId());
        assertEquals(10, result.getAvailableQuantity());
    }

    @Test
    void testUpdateInventory_TotalLessThanReserved() {
        InventoryServiceImpl service = new InventoryServiceImpl(
                inventoryItemRepository, stockReservationRepository, processedKafkaEventRepository,
                kafkaEventPublisher, new InventoryMapper(), "res", "fail", "rel"
        );
        InventoryItem existing = new InventoryItem();
        existing.setProductId("P1");
        existing.setReservedQuantity(5);
        when(inventoryItemRepository.findByProductId("P1")).thenReturn(Optional.of(existing));

        InventoryUpdateRequest request = new InventoryUpdateRequest();
        request.setProductName("Updated");
        request.setTotalQuantity(3); // Less than reserved 5
        request.setLowStockThreshold(1);
        request.setActive(true);

        assertThrows(InventoryDomainException.class, () -> service.updateInventory("P1", request));
    }

    @Test
    void testRestock_ExistingItem() {
        InventoryServiceImpl service = new InventoryServiceImpl(
                inventoryItemRepository, stockReservationRepository, processedKafkaEventRepository,
                kafkaEventPublisher, new InventoryMapper(), "res", "fail", "rel"
        );
        InventoryItem existing = new InventoryItem();
        existing.setProductId("P1");
        existing.setTotalQuantity(10);
        existing.setAvailableQuantity(8);
        when(inventoryItemRepository.findByProductId("P1")).thenReturn(Optional.of(existing));
        when(inventoryItemRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        RestockRequest request = new RestockRequest();
        request.setQuantity(5);
        InventoryResponse result = service.restock("P1", request);
        assertEquals(15, result.getTotalQuantity());
        assertEquals(13, result.getAvailableQuantity());
    }

    @Test
    void testRestock_NewItem() {
        InventoryServiceImpl service = new InventoryServiceImpl(
                inventoryItemRepository, stockReservationRepository, processedKafkaEventRepository,
                kafkaEventPublisher, new InventoryMapper(), "res", "fail", "rel"
        );
        when(inventoryItemRepository.findByProductId("P1")).thenReturn(Optional.empty());
        when(inventoryItemRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        RestockRequest request = new RestockRequest();
        request.setQuantity(10);
        InventoryResponse result = service.restock("P1", request);
        assertEquals(10, result.getTotalQuantity());
        assertEquals(10, result.getAvailableQuantity());
    }

    @Test
    void testGetLowStockInventory() {
        InventoryServiceImpl service = new InventoryServiceImpl(
                inventoryItemRepository, stockReservationRepository, processedKafkaEventRepository,
                kafkaEventPublisher, new InventoryMapper(), "res", "fail", "rel"
        );
        InventoryItem lowStockItem = new InventoryItem();
        lowStockItem.setProductId("P1");
        lowStockItem.setProductName("Low");
        lowStockItem.setTotalQuantity(10);
        lowStockItem.setAvailableQuantity(1);
        lowStockItem.setReservedQuantity(9);
        lowStockItem.setLowStockThreshold(5);
        lowStockItem.setActive(true);

        InventoryItem normalItem = new InventoryItem();
        normalItem.setProductId("P2");
        normalItem.setProductName("Normal");
        normalItem.setTotalQuantity(10);
        normalItem.setAvailableQuantity(8);
        normalItem.setReservedQuantity(2);
        normalItem.setLowStockThreshold(3);
        normalItem.setActive(true);

        when(inventoryItemRepository.findAllByOrderByProductIdAsc()).thenReturn(List.of(lowStockItem, normalItem));
        List<InventorySummaryResponse> result = service.getLowStockInventory();
        assertEquals(1, result.size());
        assertEquals("P1", result.get(0).getProductId());
    }

    @Test
    void testGetOrderStock() {
        InventoryServiceImpl service = new InventoryServiceImpl(
                inventoryItemRepository, stockReservationRepository, processedKafkaEventRepository,
                kafkaEventPublisher, new InventoryMapper(), "res", "fail", "rel"
        );
        StockReservation reservation = new StockReservation();
        reservation.setOrderId("O1");
        reservation.setProductId("P1");
        reservation.setQuantity(2);
        reservation.setStatus(ReservationStatus.RESERVED);
        reservation.setReason("reason");
        reservation.setCreatedAt(LocalDateTime.now());

        InventoryItem item = new InventoryItem();
        item.setProductId("P1");
        item.setProductName("Widget");
        item.setTotalQuantity(10);
        item.setAvailableQuantity(8);
        item.setReservedQuantity(2);
        item.setActive(true);

        when(stockReservationRepository.findByOrderIdOrderByCreatedAtDesc("O1")).thenReturn(List.of(reservation));
        when(inventoryItemRepository.findByProductId("P1")).thenReturn(Optional.of(item));

        OrderStockResponse result = service.getOrderStock("O1");
        assertEquals("O1", result.getOrderId());
        assertEquals(1, result.getItems().size());
    }

    @Test
    void testGetOrderStock_NullOrderId() {
        InventoryServiceImpl service = new InventoryServiceImpl(
                inventoryItemRepository, stockReservationRepository, processedKafkaEventRepository,
                kafkaEventPublisher, new InventoryMapper(), "res", "fail", "rel"
        );
        assertThrows(OrderStockNotFoundException.class, () -> service.getOrderStock(null));
    }

    @Test
    void testGetOrderStock_BlankOrderId() {
        InventoryServiceImpl service = new InventoryServiceImpl(
                inventoryItemRepository, stockReservationRepository, processedKafkaEventRepository,
                kafkaEventPublisher, new InventoryMapper(), "res", "fail", "rel"
        );
        assertThrows(OrderStockNotFoundException.class, () -> service.getOrderStock("  "));
    }

    @Test
    void testGetOrderStock_NoReservations() {
        InventoryServiceImpl service = new InventoryServiceImpl(
                inventoryItemRepository, stockReservationRepository, processedKafkaEventRepository,
                kafkaEventPublisher, new InventoryMapper(), "res", "fail", "rel"
        );
        when(stockReservationRepository.findByOrderIdOrderByCreatedAtDesc("O1")).thenReturn(Collections.emptyList());
        assertThrows(OrderStockNotFoundException.class, () -> service.getOrderStock("O1"));
    }

    @Test
    void testProcessPaymentProcessedEvent_SuccessPath() {
        InventoryServiceImpl service = new InventoryServiceImpl(
                inventoryItemRepository, stockReservationRepository, processedKafkaEventRepository,
                kafkaEventPublisher, new InventoryMapper(), "res", "fail", "rel"
        );
        PaymentProcessedEvent event = new PaymentProcessedEvent();
        event.setEventId("E1");
        event.setOrderId("O1");
        PaymentItemEvent itemEvent = new PaymentItemEvent();
        itemEvent.setProductId("P1");
        itemEvent.setQuantity(2);
        event.setItems(List.of(itemEvent));

        InventoryItem inv = new InventoryItem();
        inv.setProductId("P1");
        inv.setAvailableQuantity(10);
        inv.setReservedQuantity(0);

        when(processedKafkaEventRepository.findByEventId("E1")).thenReturn(Optional.empty());
        when(inventoryItemRepository.findByProductId("P1")).thenReturn(Optional.of(inv));
        when(inventoryItemRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(stockReservationRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        service.processPaymentProcessedEvent(event);
        verify(kafkaEventPublisher).publish(eq("res"), any());
        verify(processedKafkaEventRepository).save(any());
    }

    @Test
    void testProcessPaymentProcessedEvent_SuccessMultipleItems() {
        InventoryServiceImpl service = new InventoryServiceImpl(
                inventoryItemRepository, stockReservationRepository, processedKafkaEventRepository,
                kafkaEventPublisher, new InventoryMapper(), "res", "fail", "rel"
        );
        PaymentProcessedEvent event = new PaymentProcessedEvent();
        event.setEventId("E2");
        event.setOrderId("O2");
        PaymentItemEvent item1 = new PaymentItemEvent();
        item1.setProductId("P1");
        item1.setQuantity(1);
        PaymentItemEvent item2 = new PaymentItemEvent();
        item2.setProductId("P2");
        item2.setQuantity(3);
        event.setItems(List.of(item1, item2));

        InventoryItem inv1 = new InventoryItem();
        inv1.setProductId("P1");
        inv1.setAvailableQuantity(5);
        inv1.setReservedQuantity(0);

        InventoryItem inv2 = new InventoryItem();
        inv2.setProductId("P2");
        inv2.setAvailableQuantity(10);
        inv2.setReservedQuantity(0);

        when(processedKafkaEventRepository.findByEventId("E2")).thenReturn(Optional.empty());
        when(inventoryItemRepository.findByProductId("P1")).thenReturn(Optional.of(inv1));
        when(inventoryItemRepository.findByProductId("P2")).thenReturn(Optional.of(inv2));
        when(inventoryItemRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(stockReservationRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        service.processPaymentProcessedEvent(event);
        verify(kafkaEventPublisher, times(2)).publish(eq("res"), any());
    }

    @Test
    void testProcessPaymentProcessedEvent_BlankEventId() {
        InventoryServiceImpl service = new InventoryServiceImpl(
                inventoryItemRepository, stockReservationRepository, processedKafkaEventRepository,
                kafkaEventPublisher, new InventoryMapper(), "res", "fail", "rel"
        );
        PaymentProcessedEvent event = new PaymentProcessedEvent();
        event.setEventId("   ");
        event.setOrderId("O1");
        PaymentItemEvent itemEvent = new PaymentItemEvent();
        itemEvent.setProductId("P1");
        itemEvent.setQuantity(1);
        event.setItems(List.of(itemEvent));

        InventoryItem inv = new InventoryItem();
        inv.setProductId("P1");
        inv.setAvailableQuantity(10);
        inv.setReservedQuantity(0);

        when(processedKafkaEventRepository.findByEventId(any())).thenReturn(Optional.empty());
        when(inventoryItemRepository.findByProductId("P1")).thenReturn(Optional.of(inv));
        when(inventoryItemRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(stockReservationRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        service.processPaymentProcessedEvent(event);
        verify(kafkaEventPublisher).publish(eq("res"), any());
    }

    @Test
    void testProcessPaymentProcessedEvent_NullQuantity() {
        InventoryServiceImpl service = new InventoryServiceImpl(
                inventoryItemRepository, stockReservationRepository, processedKafkaEventRepository,
                kafkaEventPublisher, new InventoryMapper(), "res", "fail", "rel"
        );
        PaymentProcessedEvent event = new PaymentProcessedEvent();
        event.setOrderId("O1");
        PaymentItemEvent itemEvent = new PaymentItemEvent();
        itemEvent.setProductId("P1");
        itemEvent.setQuantity(null);
        event.setItems(List.of(itemEvent));

        InventoryItem inv = new InventoryItem();
        inv.setProductId("P1");
        inv.setAvailableQuantity(10);
        when(processedKafkaEventRepository.findByEventId(any())).thenReturn(Optional.empty());
        when(inventoryItemRepository.findByProductId("P1")).thenReturn(Optional.of(inv));

        service.processPaymentProcessedEvent(event);
        verify(kafkaEventPublisher).publish(eq("fail"), any());
    }

    @Test
    void testProcessPaymentProcessedEvent_NullItems() {
        InventoryServiceImpl service = new InventoryServiceImpl(
                inventoryItemRepository, stockReservationRepository, processedKafkaEventRepository,
                kafkaEventPublisher, new InventoryMapper(), "res", "fail", "rel"
        );
        PaymentProcessedEvent event = new PaymentProcessedEvent();
        event.setOrderId("O1");
        event.setItems(null);
        service.processPaymentProcessedEvent(event);
        verify(kafkaEventPublisher).publish(eq("fail"), any());
    }

    @Test
    void testReleaseInventoryForOrder_SuccessPath() {
        InventoryServiceImpl service = new InventoryServiceImpl(
                inventoryItemRepository, stockReservationRepository, processedKafkaEventRepository,
                kafkaEventPublisher, new InventoryMapper(), "res", "fail", "rel"
        );
        OrderCancelledEvent event = new OrderCancelledEvent();
        event.setOrderId("O1");
        event.setEventId("EV1");
        event.setReason("Customer cancelled");

        StockReservation res = new StockReservation();
        res.setProductId("P1");
        res.setQuantity(2);
        res.setStatus(ReservationStatus.RESERVED);

        InventoryItem inv = new InventoryItem();
        inv.setProductId("P1");
        inv.setAvailableQuantity(5);
        inv.setReservedQuantity(3);

        when(stockReservationRepository.findByOrderIdAndStatusOrderByCreatedAtDesc("O1", ReservationStatus.RESERVED))
                .thenReturn(List.of(res));
        when(inventoryItemRepository.findByProductId("P1")).thenReturn(Optional.of(inv));

        service.releaseInventoryForOrder(event);
        verify(inventoryItemRepository).save(any());
        verify(stockReservationRepository).save(any());
        verify(kafkaEventPublisher).publish(eq("rel"), any());
    }

    @Test
    void testReleaseInventoryForOrder_NoReason() {
        InventoryServiceImpl service = new InventoryServiceImpl(
                inventoryItemRepository, stockReservationRepository, processedKafkaEventRepository,
                kafkaEventPublisher, new InventoryMapper(), "res", "fail", "rel"
        );
        OrderCancelledEvent event = new OrderCancelledEvent();
        event.setOrderId("O1");
        event.setReason(null);

        StockReservation res = new StockReservation();
        res.setProductId("P1");
        res.setQuantity(2);

        InventoryItem inv = new InventoryItem();
        inv.setProductId("P1");
        inv.setAvailableQuantity(5);
        inv.setReservedQuantity(3);

        when(stockReservationRepository.findByOrderIdAndStatusOrderByCreatedAtDesc("O1", ReservationStatus.RESERVED))
                .thenReturn(List.of(res));
        when(inventoryItemRepository.findByProductId("P1")).thenReturn(Optional.of(inv));

        service.releaseInventoryForOrder(event);
        verify(kafkaEventPublisher).publish(eq("rel"), any());
    }

    @Test
    void testReleaseInventoryForOrder_BlankOrderId() {
        InventoryServiceImpl service = new InventoryServiceImpl(
                inventoryItemRepository, stockReservationRepository, processedKafkaEventRepository,
                kafkaEventPublisher, new InventoryMapper(), "res", "fail", "rel"
        );
        OrderCancelledEvent event = new OrderCancelledEvent();
        event.setOrderId("  ");
        service.releaseInventoryForOrder(event);
        verifyNoInteractions(stockReservationRepository);
    }

    @Test
    void testReleaseInventoryForOrder_NullEventId() {
        InventoryServiceImpl service = new InventoryServiceImpl(
                inventoryItemRepository, stockReservationRepository, processedKafkaEventRepository,
                kafkaEventPublisher, new InventoryMapper(), "res", "fail", "rel"
        );
        OrderCancelledEvent event = new OrderCancelledEvent();
        event.setOrderId("O1");
        event.setEventId(null);

        StockReservation res = new StockReservation();
        res.setProductId("P1");
        res.setQuantity(2);

        InventoryItem inv = new InventoryItem();
        inv.setProductId("P1");
        inv.setAvailableQuantity(5);
        inv.setReservedQuantity(3);

        when(stockReservationRepository.findByOrderIdAndStatusOrderByCreatedAtDesc("O1", ReservationStatus.RESERVED))
                .thenReturn(List.of(res));
        when(inventoryItemRepository.findByProductId("P1")).thenReturn(Optional.of(inv));

        service.releaseInventoryForOrder(event);
        verify(kafkaEventPublisher).publish(eq("rel"), any());
    }

    // ---------------------------------------------------------
    // 9. InventoryController Tests
    // ---------------------------------------------------------
    @Test
    void testControllerGetAllInventory() {
        InventoryService mockService = mock(InventoryService.class);
        InventoryController controller = new InventoryController(mockService);
        when(mockService.getAllInventory()).thenReturn(Collections.emptyList());
        ResponseEntity<ApiResponse<List<InventorySummaryResponse>>> result = controller.getAllInventory();
        assertTrue(result.getBody().isSuccess());
    }

    @Test
    void testControllerCreateInventory() {
        InventoryService mockService = mock(InventoryService.class);
        InventoryController controller = new InventoryController(mockService);
        InventoryCreateRequest request = new InventoryCreateRequest();
        request.setProductId("P1");
        InventoryResponse resp = new InventoryResponse();
        resp.setProductId("P1");
        when(mockService.createInventory(request)).thenReturn(resp);
        ResponseEntity<ApiResponse<InventoryResponse>> result = controller.createInventory(request);
        assertTrue(result.getBody().isSuccess());
        assertEquals("P1", result.getBody().getData().getProductId());
    }

    @Test
    void testControllerGetInventoryByProductId() {
        InventoryService mockService = mock(InventoryService.class);
        InventoryController controller = new InventoryController(mockService);
        InventoryResponse resp = new InventoryResponse();
        resp.setProductId("P1");
        when(mockService.getInventory("P1")).thenReturn(resp);
        ResponseEntity<ApiResponse<InventoryResponse>> result = controller.getInventoryByProductId("P1");
        assertTrue(result.getBody().isSuccess());
    }

    @Test
    void testControllerUpdateInventory() {
        InventoryService mockService = mock(InventoryService.class);
        InventoryController controller = new InventoryController(mockService);
        InventoryUpdateRequest request = new InventoryUpdateRequest();
        InventoryResponse resp = new InventoryResponse();
        resp.setProductId("P1");
        when(mockService.updateInventory("P1", request)).thenReturn(resp);
        ResponseEntity<ApiResponse<InventoryResponse>> result = controller.updateInventory("P1", request);
        assertTrue(result.getBody().isSuccess());
    }

    @Test
    void testControllerRestockInventory() {
        InventoryService mockService = mock(InventoryService.class);
        InventoryController controller = new InventoryController(mockService);
        RestockRequest request = new RestockRequest();
        request.setQuantity(5);
        InventoryResponse resp = new InventoryResponse();
        resp.setProductId("P1");
        when(mockService.restock("P1", request)).thenReturn(resp);
        ResponseEntity<ApiResponse<InventoryResponse>> result = controller.restockInventory("P1", request);
        assertTrue(result.getBody().isSuccess());
    }

    @Test
    void testControllerRestockInventoryFromQuery() {
        InventoryService mockService = mock(InventoryService.class);
        InventoryController controller = new InventoryController(mockService);
        InventoryResponse resp = new InventoryResponse();
        resp.setProductId("P1");
        when(mockService.restock(eq("P1"), any())).thenReturn(resp);
        ResponseEntity<ApiResponse<InventoryResponse>> result = controller.restockInventoryFromQuery("P1", 5, "note");
        assertTrue(result.getBody().isSuccess());
    }

    @Test
    void testControllerRestockInventoryFromQuery_NullNote() {
        InventoryService mockService = mock(InventoryService.class);
        InventoryController controller = new InventoryController(mockService);
        InventoryResponse resp = new InventoryResponse();
        resp.setProductId("P1");
        when(mockService.restock(eq("P1"), any())).thenReturn(resp);
        ResponseEntity<ApiResponse<InventoryResponse>> result = controller.restockInventoryFromQuery("P1", 5, null);
        assertTrue(result.getBody().isSuccess());
    }

    @Test
    void testControllerGetLowStockInventory() {
        InventoryService mockService = mock(InventoryService.class);
        InventoryController controller = new InventoryController(mockService);
        when(mockService.getLowStockInventory()).thenReturn(Collections.emptyList());
        ResponseEntity<ApiResponse<List<InventorySummaryResponse>>> result = controller.getLowStockInventory();
        assertTrue(result.getBody().isSuccess());
    }

    @Test
    void testControllerGetOrderStock() {
        InventoryService mockService = mock(InventoryService.class);
        InventoryController controller = new InventoryController(mockService);
        OrderStockResponse osr = new OrderStockResponse();
        osr.setOrderId("O1");
        when(mockService.getOrderStock("O1")).thenReturn(osr);
        ResponseEntity<ApiResponse<OrderStockResponse>> result = controller.getOrderStock("O1");
        assertTrue(result.getBody().isSuccess());
    }

    // ---------------------------------------------------------
    // 10. Event Classes – Full Getter/Setter Coverage
    // ---------------------------------------------------------
    @Test
    void testInventoryEvent_AllFields() {
        InventoryEvent event = new InventoryEvent();
        LocalDateTime now = LocalDateTime.now();
        event.setEventId("EV1");
        event.setEventType("inventory.reserved");
        event.setOrderId("O1");
        event.setProductId("P1");
        event.setQuantity(5);
        event.setReason("Stock reserved");
        event.setCreatedAt(now);
        assertEquals("EV1", event.getEventId());
        assertEquals("inventory.reserved", event.getEventType());
        assertEquals("O1", event.getOrderId());
        assertEquals("P1", event.getProductId());
        assertEquals(5, event.getQuantity());
        assertEquals("Stock reserved", event.getReason());
        assertEquals(now, event.getCreatedAt());
    }

    @Test
    void testOrderCancelledEvent_AllFields() {
        OrderCancelledEvent event = new OrderCancelledEvent();
        event.setEventId("EV1");
        event.setOrderId("O1");
        event.setReason("Customer cancelled");
        assertEquals("EV1", event.getEventId());
        assertEquals("O1", event.getOrderId());
        assertEquals("Customer cancelled", event.getReason());
    }

    @Test
    void testPaymentProcessedEvent_AllFields() {
        PaymentProcessedEvent event = new PaymentProcessedEvent();
        event.setEventId("EV1");
        event.setOrderId("O1");
        event.setPaymentId("PAY1");
        PaymentItemEvent item = new PaymentItemEvent();
        item.setProductId("P1");
        item.setQuantity(3);
        event.setItems(List.of(item));
        assertEquals("EV1", event.getEventId());
        assertEquals("O1", event.getOrderId());
        assertEquals("PAY1", event.getPaymentId());
        assertEquals(1, event.getItems().size());
        assertEquals("P1", event.getItems().get(0).getProductId());
        assertEquals(3, event.getItems().get(0).getQuantity());
    }

    @Test
    void testPaymentItemEvent_AllFields() {
        PaymentItemEvent item = new PaymentItemEvent();
        item.setProductId("P1");
        item.setQuantity(10);
        assertEquals("P1", item.getProductId());
        assertEquals(10, item.getQuantity());
    }

    // ---------------------------------------------------------
    // 11. KafkaConfig Bean Tests
    // ---------------------------------------------------------
    @Test
    void testKafkaConfig_beans() {
        KafkaConfig kafkaConfig = new KafkaConfig();

        // Use reflection to set @Value fields
        setField(kafkaConfig, "paymentProcessedTopic", "payment-processed");
        setField(kafkaConfig, "inventoryReservedTopic", "inventory-reserved");
        setField(kafkaConfig, "inventoryFailedTopic", "inventory-failed");
        setField(kafkaConfig, "inventoryReleasedTopic", "inventory-released");
        setField(kafkaConfig, "orderCancelledTopic", "order-cancelled");

        KafkaProperties kafkaProperties = new KafkaProperties();
        ObjectProvider<SslBundles> sslBundlesProvider = mock(ObjectProvider.class);
        when(sslBundlesProvider.getIfAvailable()).thenReturn(null);

        ProducerFactory<String, String> producerFactory = kafkaConfig.producerFactory(kafkaProperties, sslBundlesProvider);
        assertNotNull(producerFactory);

        KafkaTemplate<String, String> template = kafkaConfig.kafkaTemplate(producerFactory);
        assertNotNull(template);

        ConsumerFactory<String, String> consumerFactory = kafkaConfig.consumerFactory(kafkaProperties, sslBundlesProvider);
        assertNotNull(consumerFactory);

        ConcurrentKafkaListenerContainerFactory<String, String> factory = kafkaConfig.kafkaListenerContainerFactory(consumerFactory);
        assertNotNull(factory);

        KafkaAdmin kafkaAdmin = kafkaConfig.kafkaAdmin(kafkaProperties, sslBundlesProvider);
        assertNotNull(kafkaAdmin);

        KafkaAdmin.NewTopics topics = kafkaConfig.kafkaTopics();
        assertNotNull(topics);
    }

    @Test
    void testKafkaConfig_kafkaAdminWithTimeouts() {
        KafkaConfig kafkaConfig = new KafkaConfig();
        setField(kafkaConfig, "paymentProcessedTopic", "t1");
        setField(kafkaConfig, "inventoryReservedTopic", "t2");
        setField(kafkaConfig, "inventoryFailedTopic", "t3");
        setField(kafkaConfig, "inventoryReleasedTopic", "t4");
        setField(kafkaConfig, "orderCancelledTopic", "t5");

        KafkaProperties kafkaProperties = new KafkaProperties();
        KafkaProperties.Admin admin = kafkaProperties.getAdmin();
        admin.setCloseTimeout(java.time.Duration.ofSeconds(30));
        admin.setOperationTimeout(java.time.Duration.ofSeconds(60));
        admin.setFailFast(true);
        admin.setAutoCreate(true);
        admin.setModifyTopicConfigs(true);

        ObjectProvider<SslBundles> sslBundlesProvider = mock(ObjectProvider.class);
        when(sslBundlesProvider.getIfAvailable()).thenReturn(null);

        KafkaAdmin kafkaAdmin = kafkaConfig.kafkaAdmin(kafkaProperties, sslBundlesProvider);
        assertNotNull(kafkaAdmin);
    }

    // ---------------------------------------------------------
    // 12. ApiResponse static factory methods
    // ---------------------------------------------------------
    @Test
    void testApiResponse_failureFactory() {
        ApiResponse<Object> errorResp = ApiResponse.failure("Error occurred", "ERR_CODE", null);
        assertFalse(errorResp.isSuccess());
        assertEquals("Error occurred", errorResp.getMessage());
        assertEquals("ERR_CODE", errorResp.getErrorCode());
        assertNull(errorResp.getData());
    }

    @Test
    void testApiResponse_failureNullErrorCode() {
        ApiResponse<Object> errorResp = ApiResponse.failure("Error occurred", null, null);
        assertFalse(errorResp.isSuccess());
        assertEquals("", errorResp.getErrorCode());
    }

    @Test
    void testApiResponse_timestamp() {
        ApiResponse<String> resp = ApiResponse.success("msg", "data");
        assertNotNull(resp.getTimestamp());
        LocalDateTime newTime = LocalDateTime.now();
        resp.setTimestamp(newTime);
        assertEquals(newTime, resp.getTimestamp());
    }

    // ---------------------------------------------------------
    // 13. InventoryServiceApplication main method
    // ---------------------------------------------------------
    @Test
    void testInventoryServiceApplicationMain() {
        try (var mockedSpringApp = mockStatic(org.springframework.boot.SpringApplication.class)) {
            mockedSpringApp.when(() -> org.springframework.boot.SpringApplication.run(eq(InventoryServiceApplication.class), any(String[].class)))
                    .thenReturn(null);
            
            InventoryServiceApplication.main(new String[]{});
            
            mockedSpringApp.verify(() -> org.springframework.boot.SpringApplication.run(eq(InventoryServiceApplication.class), any(String[].class)));
        }
    }

    // ---------------------------------------------------------
    // Helper method for reflection
    // ---------------------------------------------------------
    private void setField(Object target, String fieldName, Object value) {
        try {
            java.lang.reflect.Field field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (Exception e) {
            throw new RuntimeException("Failed to set field: " + fieldName, e);
        }
    }

    // ---------------------------------------------------------
    // 14. Edge Case Coverage – Remaining branches
    // ---------------------------------------------------------

    @Test
    void testMapper_toReservationResponse_NullStatus() {
        InventoryMapper mapper = new InventoryMapper();
        StockReservation res = new StockReservation();
        res.setOrderId("O1");
        res.setProductId("P1");
        res.setQuantity(1);
        res.setStatus(null);
        ReservationResponse result = mapper.toReservationResponse(res);
        assertNull(result.getStatus());
    }

    @Test
    void testMapper_toOrderStockItemResponse_NullStatus() {
        InventoryMapper mapper = new InventoryMapper();
        StockReservation res = new StockReservation();
        res.setOrderId("O1");
        res.setProductId("P1");
        res.setQuantity(1);
        res.setStatus(null);
        OrderStockItemResponse result = mapper.toOrderStockItemResponse(res, null);
        assertNull(result.getStatus());
    }

    @Test
    void testReleaseInventoryForOrder_NullOrderIdInEvent() {
        InventoryServiceImpl service = new InventoryServiceImpl(
                inventoryItemRepository, stockReservationRepository, processedKafkaEventRepository,
                kafkaEventPublisher, new InventoryMapper(), "res", "fail", "rel"
        );
        OrderCancelledEvent event = new OrderCancelledEvent();
        event.setOrderId(null);
        service.releaseInventoryForOrder(event);
        verifyNoInteractions(stockReservationRepository);
    }

    @Test
    void testUpdateInventory_NullReservedQuantity() {
        InventoryServiceImpl service = new InventoryServiceImpl(
                inventoryItemRepository, stockReservationRepository, processedKafkaEventRepository,
                kafkaEventPublisher, new InventoryMapper(), "res", "fail", "rel"
        );
        InventoryItem existing = new InventoryItem();
        existing.setProductId("P1");
        existing.setReservedQuantity(null);
        when(inventoryItemRepository.findByProductId("P1")).thenReturn(Optional.of(existing));
        when(inventoryItemRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        InventoryUpdateRequest request = new InventoryUpdateRequest();
        request.setProductName("Updated");
        request.setTotalQuantity(10);
        request.setLowStockThreshold(2);
        request.setActive(true);

        InventoryResponse result = service.updateInventory("P1", request);
        assertEquals(10, result.getAvailableQuantity());
    }

    @Test
    void testRestock_NullQuantities() {
        InventoryServiceImpl service = new InventoryServiceImpl(
                inventoryItemRepository, stockReservationRepository, processedKafkaEventRepository,
                kafkaEventPublisher, new InventoryMapper(), "res", "fail", "rel"
        );
        InventoryItem existing = new InventoryItem();
        existing.setProductId("P1");
        existing.setTotalQuantity(null);
        existing.setAvailableQuantity(null);
        when(inventoryItemRepository.findByProductId("P1")).thenReturn(Optional.of(existing));
        when(inventoryItemRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        RestockRequest request = new RestockRequest();
        request.setQuantity(5);
        InventoryResponse result = service.restock("P1", request);
        assertEquals(5, result.getTotalQuantity());
        assertEquals(5, result.getAvailableQuantity());
    }

    @Test
    void testProcessPaymentProcessedEvent_NullAvailableQuantity() {
        InventoryServiceImpl service = new InventoryServiceImpl(
                inventoryItemRepository, stockReservationRepository, processedKafkaEventRepository,
                kafkaEventPublisher, new InventoryMapper(), "res", "fail", "rel"
        );
        PaymentProcessedEvent event = new PaymentProcessedEvent();
        event.setEventId("E1");
        event.setOrderId("O1");
        PaymentItemEvent itemEvent = new PaymentItemEvent();
        itemEvent.setProductId("P1");
        itemEvent.setQuantity(1);
        event.setItems(List.of(itemEvent));

        InventoryItem inv = new InventoryItem();
        inv.setProductId("P1");
        inv.setAvailableQuantity(null); // null available
        when(processedKafkaEventRepository.findByEventId("E1")).thenReturn(Optional.empty());
        when(inventoryItemRepository.findByProductId("P1")).thenReturn(Optional.of(inv));

        service.processPaymentProcessedEvent(event);
        verify(kafkaEventPublisher).publish(eq("fail"), any()); // OUT_OF_STOCK since 0 < 1
    }

    @Test
    void testGetOrderStock_WithNullInventoryItem() {
        InventoryServiceImpl service = new InventoryServiceImpl(
                inventoryItemRepository, stockReservationRepository, processedKafkaEventRepository,
                kafkaEventPublisher, new InventoryMapper(), "res", "fail", "rel"
        );
        StockReservation reservation = new StockReservation();
        reservation.setOrderId("O1");
        reservation.setProductId("P1");
        reservation.setQuantity(2);
        reservation.setStatus(ReservationStatus.RESERVED);
        reservation.setCreatedAt(LocalDateTime.now());

        when(stockReservationRepository.findByOrderIdOrderByCreatedAtDesc("O1")).thenReturn(List.of(reservation));
        when(inventoryItemRepository.findByProductId("P1")).thenReturn(Optional.empty());

        OrderStockResponse result = service.getOrderStock("O1");
        assertEquals("O1", result.getOrderId());
        assertEquals("P1", result.getItems().get(0).getProductName()); // Falls back to productId
    }

    @Test
    void testApiResponse_noArgConstructor() {
        ApiResponse<String> resp = new ApiResponse<>();
        assertFalse(resp.isSuccess());
        assertNull(resp.getMessage());
        assertNull(resp.getData());
        assertNull(resp.getTimestamp());
        assertNull(resp.getErrorCode());
    }

    @Test
    void testNonBlankEventIdFromOrder_Reflection() throws Exception {
        InventoryServiceImpl service = new InventoryServiceImpl(
                inventoryItemRepository, stockReservationRepository, processedKafkaEventRepository,
                kafkaEventPublisher, new InventoryMapper(), "res", "fail", "rel"
        );
        java.lang.reflect.Method method = InventoryServiceImpl.class.getDeclaredMethod("nonBlankEventIdFromOrder", String.class);
        method.setAccessible(true);
        
        String nullResult = (String) method.invoke(service, (String) null);
        assertTrue(nullResult.startsWith("order-"));
        
        String blankResult = (String) method.invoke(service, "   ");
        assertTrue(blankResult.startsWith("order-"));
        
        String validResult = (String) method.invoke(service, "myOrderId");
        assertEquals("myOrderId", validResult);
    }

    private void dummyMethod(String param) {}
}
