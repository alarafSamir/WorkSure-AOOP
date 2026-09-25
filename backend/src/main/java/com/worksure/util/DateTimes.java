package com.worksure.util;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

public final class DateTimes {
    private static final DateTimeFormatter MYSQL = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private DateTimes() {}

    public static String toMysqlDatetime(Object value) {
        if (value == null || String.valueOf(value).isBlank()) {
            return null;
        }
        if (value instanceof LocalDateTime ldt) {
            return ldt.format(MYSQL);
        }
        Instant instant;
        try {
            instant = Instant.parse(String.valueOf(value));
        } catch (Exception e) {
            String s = String.valueOf(value).trim().replace(' ', 'T');
            if (!s.endsWith("Z") && s.length() == 19) {
                return LocalDateTime.parse(s).format(MYSQL);
            }
            try {
                instant = Instant.parse(s.endsWith("Z") ? s : s + "Z");
            } catch (Exception e2) {
                throw new IllegalArgumentException("Invalid datetime value");
            }
        }
        return LocalDateTime.ofInstant(instant, ZoneOffset.UTC).format(MYSQL);
    }

    public static String defaultScheduledAt() {
        return toMysqlDatetime(Instant.now().plusSeconds(86400).toString());
    }
}
