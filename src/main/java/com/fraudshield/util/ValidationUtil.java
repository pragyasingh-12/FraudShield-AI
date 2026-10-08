package com.fraudshield.util;

import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;

/** Small input-parsing helpers used by servlets (never trust request parameters). */
public final class ValidationUtil {
    private ValidationUtil() {
    }

    public static String trim(String s) {
        return s == null ? "" : s.trim();
    }

    public static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    /** Parses a number; returns null if blank or invalid. */
    public static Double parseDouble(String s) {
        if (isBlank(s)) return null;
        try {
            double d = Double.parseDouble(s.trim().replace(",", ""));
            return Double.isNaN(d) || Double.isInfinite(d) ? null : d;
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    public static int parseInt(String s, int fallback) {
        try {
            return Integer.parseInt(s.trim());
        } catch (Exception ex) {
            return fallback;
        }
    }

    /** Parses the value of an HTML datetime-local input (yyyy-MM-ddTHH:mm); null if invalid. */
    public static LocalDateTime parseDateTime(String s) {
        if (isBlank(s)) return null;
        try {
            return LocalDateTime.parse(s.trim());
        } catch (DateTimeParseException ex) {
            return null;
        }
    }

    /** Strips control characters and caps the length of free text before it is stored (output is escaped by c:out). */
    public static String clean(String s, int maxLen) {
        String t = trim(s).replaceAll("[\\p{Cntrl}]", " ");
        return t.length() > maxLen ? t.substring(0, maxLen) : t;
    }
}
