//package com.ecommerce.authservice.controller;
//
//import com.ecommerce.authservice.model.*;
//import com.ecommerce.authservice.service.AuthService;
//import com.ecommerce.authservice.service.KeycloakLogoutService;
//import com.ecommerce.authservice.util.TokenUtil;
//import jakarta.servlet.http.HttpServletRequest;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.api.extension.ExtendWith;
//
//import org.mockito.InjectMocks;
//import org.mockito.Mock;
//import org.mockito.junit.jupiter.MockitoExtension;
//
//import org.springframework.http.ResponseEntity;
//import org.springframework.http.HttpStatus;
//
//import static org.junit.jupiter.api.Assertions.*;
//import static org.mockito.Mockito.*;
//
//@ExtendWith(MockitoExtension.class)
//class AuthControllerTest {
//
//    @Mock
//    private AuthService authService;
//
//    @Mock
//    private KeycloakLogoutService logoutService;
//
//    @Mock
//    private TokenUtil tokenUtil;
//
//    @Mock
//    private HttpServletRequest httpServletRequest;
//
//    @InjectMocks
//    private AuthController authController;
//
//    @Mock
//    private TokenResponse tokenResponse;
//
//    @BeforeEach
//    public void setup() {
//        String access_token = "jwttokenfortest";
//        String refresh_token = "testtokenfrmjwt";
//        int expire_in = 30;
//        tokenResponse = new TokenResponse();
//        tokenResponse.setAccess_token(access_token);
//        tokenResponse.setRefresh_token(refresh_token);
//        tokenResponse.setExpires_in(expire_in);
//    }
//
//    // ✅ registerAdmin test
//    @Test
//    void testRegisterAdmin_Success() {
//        RegisterRequest request = new RegisterRequest();
//        Object mockResponse = "Admin Registered";
//
//        when(authService.registerAdmin(request, "admin")).thenReturn("Admin Registered");
//
//        ResponseEntity<?> response = authController.registerAdmin(request);
//
//        assertEquals(HttpStatus.OK, response.getStatusCode());
//        assertEquals(mockResponse, response.getBody());
//
//        verify(authService, times(1)).registerAdmin(request, "admin");
//    }
//
//    // ✅ registerUser test
//    @Test
//    void testRegisterUser_Success() {
//        RegisterRequest request = new RegisterRequest();
//        String mockResponse = "User Registered";
//
//        when(authService.registerUser(request, "user")).thenReturn(mockResponse);
//
//        ResponseEntity<?> response = authController.registerUser(request);
//
//        assertEquals(HttpStatus.OK, response.getStatusCode());
//        assertEquals(mockResponse, response.getBody());
//
//        verify(authService, times(1)).registerUser(request, "user");
//    }
//
//    // ✅ login test
//    @Test
//    void testLogin_Success() {
//        LoginRequest request = new LoginRequest();
////        String mockResponse = "JWT_TOKEN";
//
//
//        when(authService.login(request)).thenReturn(tokenResponse);
//
//        ResponseEntity<?> response = authController.login(request);
//
//        assertEquals(HttpStatus.OK, response.getStatusCode());
//        assertEquals(tokenResponse, response.getBody());
//
//        verify(authService, times(1)).login(request);
//    }
//
//    // ✅ refresh test
//    @Test
//    void testRefresh_Success() {
//        RefreshRequest request = new RefreshRequest();
////        Object mockResponse = "REFRESHED_TOKEN";
//
//        String access_token = "jwttokenfortest";
//        String refresh_token = "testtokenfrmjwt";
//        int expire_in = 30;
//        tokenResponse = new TokenResponse();
//        tokenResponse.setAccess_token(access_token);
//        tokenResponse.setRefresh_token(refresh_token);
//        tokenResponse.setExpires_in(expire_in);
//
//        when(authService.refresh(request)).thenReturn(tokenResponse);
//
//        ResponseEntity<?> response = authController.refresh(request);
//
//        assertEquals(HttpStatus.OK, response.getStatusCode());
//        assertEquals(tokenResponse, response.getBody());
//
//        verify(authService, times(1)).refresh(request);
//    }
//
//    // ✅ logout success
//    @Test
//    void testLogout_Success() {
//        LogoutRequest request = new LogoutRequest();
//        request.setRefresh_token("refresh-token");
//
//        when(tokenUtil.extractAccessToken(httpServletRequest))
//                .thenReturn("access-token");
//
//        ResponseEntity<?> response =
//                authController.logout(request, httpServletRequest);
//
//        assertEquals(HttpStatus.OK, response.getStatusCode());
//        assertEquals("Logout successful. Tokens invalidated.", response.getBody());
//
//        verify(logoutService, times(1))
//                .logout("refresh-token");
//    }
//
//    // ✅ logout - missing access token
//    @Test
//    void testLogout_MissingAccessToken() {
//        LogoutRequest request = new LogoutRequest();
//        request.setRefresh_token("refresh-token");
//
//        when(tokenUtil.extractAccessToken(httpServletRequest))
//                .thenReturn(null);
//
//        ResponseEntity<?> response =
//                authController.logout(request, httpServletRequest);
//
//        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
//        assertEquals("Missing access token", response.getBody());
//
//        verify(logoutService, never()).logout(any());
//    }
//
//    // ✅ logout - missing refresh token
//    @Test
//    void testLogout_MissingRefreshToken() {
//        LogoutRequest request = new LogoutRequest(); // no refresh token
//
//        when(tokenUtil.extractAccessToken(httpServletRequest))
//                .thenReturn("access-token");
//
//        ResponseEntity<?> response =
//                authController.logout(request, httpServletRequest);
//
//        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
//        assertEquals("Missing refresh token", response.getBody());
//
//        verify(logoutService, never()).logout(any());
//    }
//}