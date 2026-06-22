package com.ecommerce.order_service.controller;

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

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'ADMIN')")
    public ResponseEntity<OrderResponseDto> getOrderById(
            @PathVariable Long id,
            Authentication auth) {

        String userId = auth.getName();
        boolean isAdmin = hasRole(auth, "ROLE_ADMIN");
        log.info("GET /api/orders/{} — user={} isAdmin={}", id, userId, isAdmin);

        return ResponseEntity.ok(orderService.getOrderById(id, userId, isAdmin));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'ADMIN')")
    public ResponseEntity<OrderResponseDto> cancelOrder(
            @PathVariable Long id,
            Authentication auth) {

        String userId = auth.getName();
        boolean isAdmin = hasRole(auth, "ROLE_ADMIN");
        log.info("DELETE /api/orders/{} — user={} isAdmin={}", id, userId, isAdmin);

        return ResponseEntity.ok(orderService.cancelOrder(id, userId, isAdmin));
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<OrderResponseDto>> getAllOrders() {
        log.info("GET /api/orders — admin fetching all orders");
        return ResponseEntity.ok(orderService.getAllOrders());
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<OrderResponseDto> updateOrderStatus(
            @PathVariable Long id,
            @Valid @RequestBody StatusUpdateRequestDto requestDto) {

        log.info("PUT /api/orders/{}/status — newStatus={}", id, requestDto.getStatus());
        return ResponseEntity.ok(orderService.updateOrderStatus(id, requestDto.getStatus()));
    }

    private boolean hasRole(Authentication auth, String role) {
        return auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(role::equals);
    }
}
