package com.ecommerce.order_service.controller;

import com.ecommerce.order_service.dto.AssignOrderRequestDto;
import com.ecommerce.order_service.dto.OrderRequestDto;
import com.ecommerce.order_service.dto.OrderResponseDto;
import com.ecommerce.order_service.dto.StatusUpdateRequestDto;
import com.ecommerce.order_service.enums.OrderStatus;
import com.ecommerce.order_service.service.OrderService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(OrderController.class)
class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private OrderService orderService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void placeOrder_Success() throws Exception {
        OrderRequestDto request = OrderRequestDto.builder()
                .items(List.of("Pizza"))
                .amount(100.0)
                .build();
        OrderResponseDto response = OrderResponseDto.builder().id(1L).orderId("uuid").build();

        when(orderService.placeOrder(any(OrderRequestDto.class), anyString())).thenReturn(response);

        mockMvc.perform(post("/api/orders")
                        .with(jwt().jwt(j -> j.subject("user-1")).authorities(new SimpleGrantedAuthority("ROLE_CUSTOMER")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.orderId").value("uuid"));
    }

    @Test
    void getMyOrders_Success() throws Exception {
        OrderResponseDto response = OrderResponseDto.builder().id(1L).orderId("uuid").build();
        when(orderService.getMyOrders("user-1")).thenReturn(List.of(response));

        mockMvc.perform(get("/api/orders/me")
                        .with(jwt().jwt(j -> j.subject("user-1")).authorities(new SimpleGrantedAuthority("ROLE_CUSTOMER"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].orderId").value("uuid"));
    }

    @Test
    void getOrderById_Success() throws Exception {
        OrderResponseDto response = OrderResponseDto.builder().id(1L).orderId("uuid").build();
        when(orderService.getOrderById(eq(1L), eq("user-1"), eq(false))).thenReturn(response);

        mockMvc.perform(get("/api/orders/1")
                        .with(jwt().jwt(j -> j.subject("user-1")).authorities(new SimpleGrantedAuthority("ROLE_CUSTOMER"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderId").value("uuid"));
    }

    @Test
    void cancelOrder_Success() throws Exception {
        OrderResponseDto response = OrderResponseDto.builder().id(1L).orderId("uuid").build();
        when(orderService.cancelOrder(eq(1L), eq("user-1"), eq(false))).thenReturn(response);

        mockMvc.perform(delete("/api/orders/1")
                        .with(jwt().jwt(j -> j.subject("user-1")).authorities(new SimpleGrantedAuthority("ROLE_CUSTOMER"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderId").value("uuid"));
    }

    @Test
    void getAllOrders_Success() throws Exception {
        OrderResponseDto response = OrderResponseDto.builder().id(1L).orderId("uuid").build();
        when(orderService.getAllOrders()).thenReturn(List.of(response));

        mockMvc.perform(get("/api/orders")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].orderId").value("uuid"));
    }

    @Test
    void updateOrderStatus_Success() throws Exception {
        OrderResponseDto response = OrderResponseDto.builder().id(1L).orderId("uuid").build();
        StatusUpdateRequestDto request = new StatusUpdateRequestDto();
        request.setStatus(OrderStatus.CONFIRMED);

        when(orderService.updateOrderStatus(eq(1L), eq(OrderStatus.CONFIRMED))).thenReturn(response);

        mockMvc.perform(put("/api/orders/1/status")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderId").value("uuid"));
    }

    @Test
    void getAssignedOrders_Success() throws Exception {
        OrderResponseDto response = OrderResponseDto.builder().id(1L).orderId("uuid").build();
        when(orderService.getOrdersAssignedToMe("exec-1")).thenReturn(List.of(response));

        mockMvc.perform(get("/api/orders/assigned")
                        .with(jwt().jwt(j -> j.subject("exec-1")).authorities(new SimpleGrantedAuthority("ROLE_DELIVERY_EXECUTIVE"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].orderId").value("uuid"));
    }

    @Test
    void assignOrder_Success() throws Exception {
        OrderResponseDto response = OrderResponseDto.builder().id(1L).orderId("uuid").build();
        AssignOrderRequestDto request = AssignOrderRequestDto.builder()
                .deliveryExecutiveId("exec-1")
                .build();

        when(orderService.assignOrderToDeliveryExecutive(eq(1L), eq("exec-1"))).thenReturn(response);

        mockMvc.perform(put("/api/orders/1/assign")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderId").value("uuid"));
    }

    @Test
    void updateDeliveryStatus_Success() throws Exception {
        OrderResponseDto response = OrderResponseDto.builder().id(1L).orderId("uuid").build();
        StatusUpdateRequestDto request = new StatusUpdateRequestDto();
        request.setStatus(OrderStatus.OUT_FOR_DELIVERY);

        when(orderService.updateDeliveryStatus(eq(1L), eq(OrderStatus.OUT_FOR_DELIVERY), eq("exec-1"))).thenReturn(response);

        mockMvc.perform(put("/api/orders/1/delivery-status")
                        .with(jwt().jwt(j -> j.subject("exec-1")).authorities(new SimpleGrantedAuthority("ROLE_DELIVERY_EXECUTIVE")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderId").value("uuid"));
    }
}
