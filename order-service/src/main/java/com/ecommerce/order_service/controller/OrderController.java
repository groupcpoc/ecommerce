package com.ecommerce.order_service.controller;

import com.ecommerce.order_service.dto.AssignOrderRequestDto;
import com.ecommerce.order_service.dto.OrderRequestDto;
import com.ecommerce.order_service.dto.OrderResponseDto;
import com.ecommerce.order_service.dto.StatusUpdateRequestDto;
import com.ecommerce.order_service.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
@Slf4j
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<OrderResponseDto> placeOrder(
            @Valid @RequestBody OrderRequestDto requestDto,
            Authentication auth) {

        String userId = auth.getName();
        log.info("POST /api/orders — user={}", userId);

        OrderResponseDto response = orderService.placeOrder(requestDto, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/me")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<List<OrderResponseDto>> getMyOrders(Authentication auth) {
        String userId = auth.getName();
        log.info("GET /api/orders/me — user={}", userId);

        return ResponseEntity.ok(orderService.getMyOrders(userId));
    }

    @GetMapping("/{orderId}")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'ADMIN')")
    public ResponseEntity<OrderResponseDto> getOrderById(
            @PathVariable String orderId,
            Authentication auth) {

        String userId = auth.getName();
        boolean isAdmin = hasRole(auth, "ROLE_ADMIN");
        log.info("GET /api/orders/{} — user={} isAdmin={}", orderId, userId, isAdmin);

        return ResponseEntity.ok(orderService.getOrderById(orderId, userId, isAdmin));
    }

    @DeleteMapping("/{orderId}")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'ADMIN')")
    public ResponseEntity<OrderResponseDto> cancelOrder(
            @PathVariable String orderId,
            Authentication auth) {

        String userId = auth.getName();
        boolean isAdmin = hasRole(auth, "ROLE_ADMIN");
        log.info("DELETE /api/orders/{} — user={} isAdmin={}", orderId, userId, isAdmin);

        return ResponseEntity.ok(orderService.cancelOrder(orderId, userId, isAdmin));
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<OrderResponseDto>> getAllOrders() {
        log.info("GET /api/orders — admin fetching all orders");
        return ResponseEntity.ok(orderService.getAllOrders());
    }

    @PutMapping("/{orderId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<OrderResponseDto> updateOrderStatus(
            @PathVariable String orderId,
            @Valid @RequestBody StatusUpdateRequestDto requestDto) {

        log.info("PUT /api/orders/{} — newStatus={}", orderId, requestDto.getStatus());
        return ResponseEntity.ok(orderService.updateOrderStatus(orderId, requestDto.getStatus()));
    }

    @GetMapping("/assigned")
    @PreAuthorize("hasRole('DELIVERY_EXECUTIVE')")
    public ResponseEntity<List<OrderResponseDto>> getAssignedOrders(Authentication auth) {
        String deliveryExecutiveId = auth.getName();
        log.info("GET /api/orders/assigned — deliveryExecutiveId={}", deliveryExecutiveId);
        return ResponseEntity.ok(orderService.getOrdersAssignedToMe(deliveryExecutiveId));
    }

    @PutMapping("/{orderId}/assign")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<OrderResponseDto> assignOrder(
            @PathVariable String orderId,
            @Valid @RequestBody AssignOrderRequestDto requestDto) {
        log.info("PUT /api/orders/{}/assign — deliveryExecutiveId={}", orderId, requestDto.getDeliveryExecutiveId());
        return ResponseEntity.ok(orderService.assignOrderToDeliveryExecutive(orderId, requestDto.getDeliveryExecutiveId()));
    }

    @PutMapping("/{orderId}/delivery-status")
    @PreAuthorize("hasRole('DELIVERY_EXECUTIVE')")
    public ResponseEntity<OrderResponseDto> updateDeliveryStatus(
            @PathVariable String orderId,
            @Valid @RequestBody StatusUpdateRequestDto requestDto,
            Authentication auth) {
        String deliveryExecutiveId = auth.getName();
        log.info("PUT /api/orders/{}/delivery-status — status={} by deliveryExecutiveId={}", orderId, requestDto.getStatus(), deliveryExecutiveId);
        return ResponseEntity.ok(orderService.updateDeliveryStatus(orderId, requestDto.getStatus(), deliveryExecutiveId));
    }

    private boolean hasRole(Authentication auth, String role) {
        return auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(role::equals);
    }
}
