package com.ecommerce.authservice.controller;

import com.ecommerce.authservice.model.LoginRequest;
import com.ecommerce.authservice.model.LogoutRequest;
import com.ecommerce.authservice.model.RefreshRequest;
import com.ecommerce.authservice.model.RegisterRequest;
import com.ecommerce.authservice.service.AuthService;
import com.ecommerce.authservice.model.*;
import com.ecommerce.authservice.service.KeycloakLogoutService;
import com.ecommerce.authservice.util.TokenUtil;
import jakarta.annotation.security.RolesAllowed;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    @Autowired
    private AuthService s;
    @Autowired
    private KeycloakLogoutService logoutService;
    @Autowired
    private TokenUtil tokenUtil;
    @PostMapping("/admin/register")
    @RolesAllowed("admin")
    public ResponseEntity<?> registerAdmin(@RequestBody RegisterRequest r) {
        return ResponseEntity.ok(s.registerAdmin(r,"admin"));
    }

    @PostMapping("/user/register")
    public ResponseEntity<?> registerUser(@RequestBody RegisterRequest r) {
        return ResponseEntity.ok(s.registerUser(r,"user"));
    }

    @PostMapping("/login")
    @RolesAllowed("user")
    public ResponseEntity<?> login(@RequestBody LoginRequest r) {

        return ResponseEntity.ok(s.login(r));
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(@RequestBody RefreshRequest r) {
        return ResponseEntity.ok(s.refresh(r));
    }


//    @PostMapping("/logout")
//    public ResponseEntity<String> logout(@RequestBody LogoutRequest request) {
//        logoutService.logout(request.getRefresh_token());
//        return ResponseEntity.ok("Logout successful");
//    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(
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



}
