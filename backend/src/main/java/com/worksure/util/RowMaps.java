package com.worksure.util;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public final class RowMaps {
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final DateTimeFormatter MYSQL_DT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final Set<String> JSON_COLUMNS = Set.of(
            "availability", "images", "data", "meta", "details"
    );

    private RowMaps() {}

    public static Map<String, Object> fromResultSet(ResultSet rs) {
        try {
            ResultSetMetaData meta = rs.getMetaData();
            int cols = meta.getColumnCount();
            Map<String, Object> row = new LinkedHashMap<>();
            for (int i = 1; i <= cols; i++) {
                String name = meta.getColumnLabel(i);
                Object value = rs.getObject(i);
                row.put(name, normalize(name, value));
            }
            return row;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static Object normalize(String column, Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Boolean b) {
            return b ? 1 : 0;
        }
        if (value instanceof Timestamp ts) {
            return ts.toInstant().toString();
        }
        if (value instanceof LocalDateTime ldt) {
            return ldt.format(MYSQL_DT);
        }
        if (value instanceof java.sql.Date d) {
            return d.toString();
        }
        if (value instanceof BigDecimal bd) {
            return bd;
        }
        if (value instanceof byte[] bytes) {
            value = new String(bytes);
        }
        if (JSON_COLUMNS.contains(column) && value instanceof String s && !s.isBlank()) {
            try {
                return MAPPER.readValue(s, Object.class);
            } catch (Exception ignored) {
                return s;
            }
        }
        return value;
    }

    public static String json(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof String s) {
            return s;
        }
        try {
            return MAPPER.writeValueAsString(value);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static int asInt(Object value, int fallback) {
        if (value == null) {
            return fallback;
        }
        if (value instanceof Number n) {
            return n.intValue();
        }
        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (Exception e) {
            return fallback;
        }
    }

    public static long asLong(Object value) {
        if (value instanceof Number n) {
            return n.longValue();
        }
        return Long.parseLong(String.valueOf(value));
    }

    public static boolean asBool(Object value) {
        if (value instanceof Boolean b) {
            return b;
        }
        if (value instanceof Number n) {
            return n.intValue() != 0;
        }
        String s = String.valueOf(value).toLowerCase();
        return "1".equals(s) || "true".equals(s);
    }
}
