package com.fraudshield.util;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/** Date/time helpers shared by models, DAOs and servlets. */
public final class DateUtil {
    private static final DateTimeFormatter DISPLAY = DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm");

    private DateUtil() {
    }

    public static String format(LocalDateTime dt) {
        return dt == null ? "-" : DISPLAY.format(dt);
    }

    public static Timestamp toTimestamp(LocalDateTime dt) {
        return dt == null ? null : Timestamp.valueOf(dt);
    }

    public static LocalDateTime fromTimestamp(Timestamp ts) {
        return ts == null ? null : ts.toLocalDateTime();
    }
}
