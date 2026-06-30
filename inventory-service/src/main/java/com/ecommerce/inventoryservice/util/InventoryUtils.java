package com.ecommerce.inventoryservice.util;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public final class InventoryUtils {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private InventoryUtils() {
        // Utility class constructor
    }

    public static String formatDateTime(LocalDateTime dateTime) {
        if (dateTime == null) {
            return "";
        }
        return dateTime.format(FORMATTER);
    }

    public static int safeInt(Integer value) {
        return value == null ? 0 : value;
    }
}
