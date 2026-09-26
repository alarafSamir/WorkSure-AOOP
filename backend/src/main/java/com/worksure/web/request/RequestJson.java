package com.worksure.web.request;

import com.fasterxml.jackson.databind.JsonNode;

/** These two existing JSON columns accept both JSON values and already-encoded JSON strings. */
public final class RequestJson {
    private RequestJson() {}

    public static String databaseValue(JsonNode value) {
        if (value == null || value.isNull()) return null;
        return value.isTextual() ? value.textValue() : value.toString();
    }
}
