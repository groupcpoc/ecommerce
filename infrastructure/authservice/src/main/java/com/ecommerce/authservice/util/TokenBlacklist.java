//package com.example.authservice.util;
//
//import java.util.Set;
//import java.util.concurrent.ConcurrentHashMap;
//
//public class TokenBlacklist {
//
//    private static final Set<String> blacklist = ConcurrentHashMap.newKeySet();
//
//    public static void blacklistToken(String token) {
//        blacklist.add(token);
//    }
//
//    public static boolean isBlacklisted(String token) {
//        return blacklist.contains(token);
//    }
//}