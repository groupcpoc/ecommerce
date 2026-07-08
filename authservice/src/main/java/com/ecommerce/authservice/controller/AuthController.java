package com.ecommerce.authservice.controller;

import com.ecommerce.authservice.model.*;
import com.ecommerce.authservice.service.AuthService;
import com.ecommerce.authservice.service.KeycloakLogoutService;
import com.ecommerce.authservice.service.SuspendedService;
import com.ecommerce.authservice.util.TokenUtil;
import jakarta.annotation.security.RolesAllowed;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    @Autowired
    private AuthService s;
    //    @Autowired
    private final KeycloakLogoutService logoutService;
    @Autowired
    private TokenUtil tokenUtil;
    //    @Autowired
    private final SuspendedService suspendedService;

    public AuthController(KeycloakLogoutService logoutService, SuspendedService suspendedService) {
        this.logoutService = logoutService;
        this.suspendedService = suspendedService;
    }

    @GetMapping("/get/usr")
    public ResponseEntity<String> fetchStringUser() {
        return ResponseEntity.ok("user is available");
    }

    @PostMapping("/post/usr")
    public ResponseEntity<String> StringUser(@RequestBody String s) {
        return ResponseEntity.ok(s + " is saving in database");
    }

    @PostMapping("/admin/register")
    @RolesAllowed("admin")
    public ResponseEntity<String> registerAdmin(@RequestBody RegisterRequest r) {
        return ResponseEntity.ok(s.registerAdmin(r, "admin"));
    }

    @PostMapping("/user/register")
    public ResponseEntity<String> registerUser(@RequestBody RegisterRequest r) {
        return ResponseEntity.ok(s.userRegistration(r, "user"));
    }

    //    @PreAuthorize("hasRole('user')")
    @PostMapping("/user/login")
    @RolesAllowed("user")
    public ResponseEntity<TokenResponse> login(@RequestBody LoginRequest r) {

        return ResponseEntity.ok(s.login(r));
    }

    @PostMapping("/user/refresh")
    public ResponseEntity<?> refresh(@RequestBody RefreshRequest r) {
        return ResponseEntity.ok(s.refresh(r));
    }


    @PutMapping("/{id}/suspend")
    @PreAuthorize("hasRole('admin')")
    public ResponseEntity<Map<String, String>> suspendUser(@PathVariable String id) {

        suspendedService.suspendUser(id);

        return ResponseEntity.ok(Map.of(
                "message", "User suspended successfully",
                "userId", id,
                "status", "SUSPENDED"
        ));
    }

    @PostMapping("/user/logout")
    public ResponseEntity<String> logout(
            @RequestBody LogoutRequest request,
            HttpServletRequest httpRequest) {

        String accessToken = tokenUtil.extractAccessToken(httpRequest);

        if (accessToken == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("Missing access token");
        }

        if (request.getRefresh_token() == null) {
            return ResponseEntity.badRequest()
                    .body("Missing refresh token");
        }

        logoutService.logout(request.getRefresh_token());

        return ResponseEntity.ok("Logout successful. Tokens invalidated.");
    }


    @GetMapping("/users")
    @RolesAllowed("admin")
    public ResponseEntity<List<RegisterRequest>> getAllUsers() {
        return ResponseEntity.ok(s.getAllUsers());
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("admin")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {

        s.deleteUser(id);
        return ResponseEntity.noContent().build();
    }


}
