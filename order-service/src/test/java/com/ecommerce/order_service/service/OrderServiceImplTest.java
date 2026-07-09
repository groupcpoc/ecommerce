package com.ecommerce.order_service.service;

import com.ecommerce.order_service.dto.OrderRequestDto;
import com.ecommerce.order_service.dto.OrderResponseDto;
import com.ecommerce.order_service.entity.Order;
import com.ecommerce.order_service.enums.OrderStatus;
import com.ecommerce.order_service.exception.InvalidOrderStateException;
import com.ecommerce.order_service.exception.ResourceNotFoundException;
import com.ecommerce.order_service.mapper.OrderMapper;
import com.ecommerce.order_service.publisher.OrderEventPublisher;
import com.ecommerce.order_service.repository.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.access.AccessDeniedException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class OrderServiceImplTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderEventPublisher eventPublisher;

    @Mock
    private OrderMapper orderMapper;

    @InjectMocks
    private OrderServiceImpl orderService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void placeOrder_Success() {
        OrderRequestDto requestDto = OrderRequestDto.builder()
                .items(List.of("Item A", "Item B"))
                .amount(200.0)
                .build();
        Order savedOrder = new Order();
        OrderResponseDto expectedResponse = OrderResponseDto.builder().orderId("uuid").build();

        when(orderRepository.save(any(Order.class))).thenReturn(savedOrder);
        when(orderMapper.toResponseDto(any(Order.class))).thenReturn(expectedResponse);

        OrderResponseDto response = orderService.placeOrder(requestDto, "user-1");

        assertNotNull(response);
        assertEquals("uuid", response.getOrderId());
        verify(orderRepository).save(any(Order.class));
        verify(eventPublisher).publishOrderCreated(anyString(), anyString(), anyString(), anyDouble());
    }

    @Test
    void getMyOrders_Success() {
        Order order = new Order();
        OrderResponseDto dto = new OrderResponseDto();
        when(orderRepository.findByUserId("user-1")).thenReturn(List.of(order));
        when(orderMapper.toResponseDto(order)).thenReturn(dto);

        List<OrderResponseDto> result = orderService.getMyOrders("user-1");

        assertEquals(1, result.size());
        assertSame(dto, result.get(0));
    }

    @Test
    void getOrderById_Success_Admin() {
        Order order = Order.builder().userId("user-1").build();
        OrderResponseDto dto = new OrderResponseDto();
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(orderMapper.toResponseDto(order)).thenReturn(dto);

        OrderResponseDto result = orderService.getOrderById(1L, "user-2", true);

        assertSame(dto, result);
    }

    @Test
    void getOrderById_Success_Owner() {
        Order order = Order.builder().userId("user-1").build();
        OrderResponseDto dto = new OrderResponseDto();
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(orderMapper.toResponseDto(order)).thenReturn(dto);

        OrderResponseDto result = orderService.getOrderById(1L, "user-1", false);

        assertSame(dto, result);
    }

    @Test
    void getOrderById_AccessDenied() {
        Order order = Order.builder().userId("user-1").build();
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        assertThrows(AccessDeniedException.class, () -> 
                orderService.getOrderById(1L, "user-2", false)
        );
    }

    @Test
    void getOrderById_NotFound() {
        when(orderRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> 
                orderService.getOrderById(1L, "user-1", true)
        );
    }

    @Test
    void cancelOrder_Success_Owner() {
        Order order = Order.builder()
                .orderId("uuid-123")
                .userId("user-1")
                .status(OrderStatus.PENDING)
                .build();
        OrderResponseDto dto = new OrderResponseDto();

        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenReturn(order);
        when(orderMapper.toResponseDto(order)).thenReturn(dto);

        OrderResponseDto result = orderService.cancelOrder(1L, "user-1", false);

        assertSame(dto, result);
        assertEquals(OrderStatus.CANCELLED, order.getStatus());
        verify(eventPublisher).publishOrderCancelled("uuid-123", "user-1", "CUSTOMER_CANCELLED");
    }

    @Test
    void cancelOrder_Success_Admin() {
        Order order = Order.builder()
                .orderId("uuid-123")
                .userId("user-1")
                .status(OrderStatus.PENDING)
                .build();
        OrderResponseDto dto = new OrderResponseDto();

        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenReturn(order);
        when(orderMapper.toResponseDto(order)).thenReturn(dto);

        OrderResponseDto result = orderService.cancelOrder(1L, "user-2", true);

        assertSame(dto, result);
        assertEquals(OrderStatus.CANCELLED, order.getStatus());
        verify(eventPublisher).publishOrderCancelled("uuid-123", "user-1", "ADMIN_CANCELLED");
    }

    @Test
    void cancelOrder_AccessDenied() {
        Order order = Order.builder().userId("user-1").status(OrderStatus.PENDING).build();
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        assertThrows(AccessDeniedException.class, () -> 
                orderService.cancelOrder(1L, "user-2", false)
        );
    }

    @Test
    void cancelOrder_InvalidState() {
        Order order = Order.builder().userId("user-1").status(OrderStatus.DELIVERED).build();
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        assertThrows(InvalidOrderStateException.class, () -> 
                orderService.cancelOrder(1L, "user-1", false)
        );
    }

    @Test
    void getAllOrders_Success() {
        Order order = new Order();
        OrderResponseDto dto = new OrderResponseDto();
        when(orderRepository.findAll()).thenReturn(List.of(order));
        when(orderMapper.toResponseDto(order)).thenReturn(dto);

        List<OrderResponseDto> result = orderService.getAllOrders();

        assertEquals(1, result.size());
        assertSame(dto, result.get(0));
    }

    @Test
    void updateOrderStatus_Success() {
        Order order = Order.builder().status(OrderStatus.PENDING).build();
        OrderResponseDto dto = new OrderResponseDto();

        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenReturn(order);
        when(orderMapper.toResponseDto(order)).thenReturn(dto);

        OrderResponseDto result = orderService.updateOrderStatus(1L, OrderStatus.CONFIRMED);

        assertSame(dto, result);
        assertEquals(OrderStatus.CONFIRMED, order.getStatus());
    }

    @Test
    void updateOrderStatus_InvalidState() {
        Order order = Order.builder().status(OrderStatus.DELIVERED).build();
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        assertThrows(InvalidOrderStateException.class, () -> 
                orderService.updateOrderStatus(1L, OrderStatus.CONFIRMED)
        );
    }

    @Test
    void getOrdersAssignedToMe_Success() {
        Order order = new Order();
        OrderResponseDto dto = new OrderResponseDto();
        when(orderRepository.findByDeliveryExecutiveId("exec-1")).thenReturn(List.of(order));
        when(orderMapper.toResponseDto(order)).thenReturn(dto);

        List<OrderResponseDto> result = orderService.getOrdersAssignedToMe("exec-1");

        assertEquals(1, result.size());
        assertSame(dto, result.get(0));
    }

    @Test
    void assignOrderToDeliveryExecutive_Success() {
        Order order = Order.builder().status(OrderStatus.CONFIRMED).build();
        OrderResponseDto dto = new OrderResponseDto();

        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenReturn(order);
        when(orderMapper.toResponseDto(order)).thenReturn(dto);

        OrderResponseDto result = orderService.assignOrderToDeliveryExecutive(1L, "exec-1");

        assertSame(dto, result);
        assertEquals("exec-1", order.getDeliveryExecutiveId());
    }

    @Test
    void assignOrderToDeliveryExecutive_InvalidState() {
        Order order = Order.builder().status(OrderStatus.CANCELLED).build();
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        assertThrows(InvalidOrderStateException.class, () -> 
                orderService.assignOrderToDeliveryExecutive(1L, "exec-1")
        );
    }

    @Test
    void updateDeliveryStatus_Success() {
        Order order = Order.builder()
                .deliveryExecutiveId("exec-1")
                .status(OrderStatus.CONFIRMED)
                .build();
        OrderResponseDto dto = new OrderResponseDto();

        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenReturn(order);
        when(orderMapper.toResponseDto(order)).thenReturn(dto);

        OrderResponseDto result = orderService.updateDeliveryStatus(1L, OrderStatus.OUT_FOR_DELIVERY, "exec-1");

        assertSame(dto, result);
        assertEquals(OrderStatus.OUT_FOR_DELIVERY, order.getStatus());
    }

    @Test
    void updateDeliveryStatus_AccessDenied() {
        Order order = Order.builder().deliveryExecutiveId("exec-2").build();
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        assertThrows(AccessDeniedException.class, () -> 
                orderService.updateDeliveryStatus(1L, OrderStatus.OUT_FOR_DELIVERY, "exec-1")
        );
    }

    @Test
    void updateDeliveryStatus_InvalidStatus() {
        Order order = Order.builder().deliveryExecutiveId("exec-1").build();
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        assertThrows(IllegalArgumentException.class, () -> 
                orderService.updateDeliveryStatus(1L, OrderStatus.PENDING, "exec-1")
        );
    }

    @Test
    void updateDeliveryStatus_InvalidState() {
        Order order = Order.builder()
                .deliveryExecutiveId("exec-1")
                .status(OrderStatus.DELIVERED)
                .build();
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        assertThrows(InvalidOrderStateException.class, () -> 
                orderService.updateDeliveryStatus(1L, OrderStatus.DELIVERY_FAILED, "exec-1")
        );
    }

    @Test
    void updateDeliveryStatus_Success_Placeholder() {
        Order order = Order.builder()
                .deliveryExecutiveId("delivery-exec-uuid")
                .status(OrderStatus.CONFIRMED)
                .build();
        OrderResponseDto dto = new OrderResponseDto();

        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenReturn(order);
        when(orderMapper.toResponseDto(order)).thenReturn(dto);

        OrderResponseDto result = orderService.updateDeliveryStatus(1L, OrderStatus.OUT_FOR_DELIVERY, "exec-1");

        assertSame(dto, result);
        assertEquals(OrderStatus.OUT_FOR_DELIVERY, order.getStatus());
    }

    @Test
    void updateDeliveryStatus_ExecutiveNull() {
        Order order = Order.builder().deliveryExecutiveId(null).build();
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        assertThrows(AccessDeniedException.class, () -> 
                orderService.updateDeliveryStatus(1L, OrderStatus.OUT_FOR_DELIVERY, "exec-1")
        );
    }

    @Test
    void updateDeliveryStatus_Success_Delivered() {
        Order order = Order.builder()
                .deliveryExecutiveId("exec-1")
                .status(OrderStatus.CONFIRMED)
                .build();
        OrderResponseDto dto = new OrderResponseDto();

        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenReturn(order);
        when(orderMapper.toResponseDto(order)).thenReturn(dto);

        OrderResponseDto result = orderService.updateDeliveryStatus(1L, OrderStatus.DELIVERED, "exec-1");

        assertSame(dto, result);
        assertEquals(OrderStatus.DELIVERED, order.getStatus());
    }

    @Test
    void updateDeliveryStatus_Success_DeliveryFailed() {
        Order order = Order.builder()
                .deliveryExecutiveId("exec-1")
                .status(OrderStatus.CONFIRMED)
                .build();
        OrderResponseDto dto = new OrderResponseDto();

        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenReturn(order);
        when(orderMapper.toResponseDto(order)).thenReturn(dto);

        OrderResponseDto result = orderService.updateDeliveryStatus(1L, OrderStatus.DELIVERY_FAILED, "exec-1");

        assertSame(dto, result);
        assertEquals(OrderStatus.DELIVERY_FAILED, order.getStatus());
    }

    @Test
    void updateDeliveryStatus_InvalidState_Cancelled() {
        Order order = Order.builder()
                .deliveryExecutiveId("exec-1")
                .status(OrderStatus.CANCELLED)
                .build();
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        assertThrows(InvalidOrderStateException.class, () -> 
                orderService.updateDeliveryStatus(1L, OrderStatus.OUT_FOR_DELIVERY, "exec-1")
        );
    }

    @Test
    void updateOrderStatus_InvalidState_Cancelled() {
        Order order = Order.builder().status(OrderStatus.CANCELLED).build();
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        assertThrows(InvalidOrderStateException.class, () -> 
                orderService.updateOrderStatus(1L, OrderStatus.CONFIRMED)
        );
    }

    @Test
    void assignOrderToDeliveryExecutive_InvalidState_Delivered() {
        Order order = Order.builder().status(OrderStatus.DELIVERED).build();
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        assertThrows(InvalidOrderStateException.class, () -> 
                orderService.assignOrderToDeliveryExecutive(1L, "exec-1")
        );
    }

    @Test
    void getOrdersAssignedToMe_Success_Placeholder() {
        Order order = new Order();
        OrderResponseDto dto = new OrderResponseDto();
        when(orderRepository.findByDeliveryExecutiveId("6ad8df34-4263-4fce-a22a-4e0acabbde94")).thenReturn(List.of(order));
        when(orderRepository.findByDeliveryExecutiveId("delivery-exec-uuid")).thenReturn(List.of(order));
        when(orderMapper.toResponseDto(order)).thenReturn(dto);

        List<OrderResponseDto> result = orderService.getOrdersAssignedToMe("6ad8df34-4263-4fce-a22a-4e0acabbde94");

        assertEquals(2, result.size());
        assertSame(dto, result.get(0));
    }
}
