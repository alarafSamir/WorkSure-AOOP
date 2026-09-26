package com.worksure.web;

import java.util.LinkedHashMap;
import java.util.Map;

public final class ApiResponses {
    private ApiResponses() {}

    public static Map<String, Object> ok() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("success", true);
        return m;
    }

    public static Map<String, Object> ok(String key, Object value) {
        Map<String, Object> m = ok();
        m.put(key, value);
        return m;
    }

    public static Map<String, Object> msg(String message) {
        Map<String, Object> m = ok();
        m.put("message", message);
        return m;
    }

    public static Map<String, Object> fail(String message) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("success", false);
        m.put("message", message);
        return m;
    }
}
