//package com.example.authservice.util;
//
//import jakarta.servlet.http.HttpServletRequest;
//
//public class ExtractTokens {
//    public static String extractToken(HttpServletRequest request) {
//        String header = request.getHeader("Authorization");
//
//        if (header != null && header.startsWith("Bearer ")) {
//            return header.substring(7);
//        }
//        return null;
//    }
//}
