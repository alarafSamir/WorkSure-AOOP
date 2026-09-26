package com.worksure.web.request;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.worksure.util.RowMaps;

import java.io.IOException;
import java.math.BigDecimal;

/** Preserve the existing flexible boolean and defaulted-integer input conventions. */
public final class RequestDeserializers {
    private RequestDeserializers() {}

    public static class Identifier extends JsonDeserializer<Long> {
        @Override
        public Long deserialize(JsonParser parser, DeserializationContext context) throws IOException {
            try {
                // Never truncate a fractional ID into a different, valid database record.
                return new BigDecimal(parser.getValueAsString().trim()).longValueExact();
            } catch (NullPointerException | NumberFormatException | ArithmeticException ex) {
                return (Long) context.handleUnexpectedToken(Long.class, parser);
            }
        }
    }

    public static class ReviewRating extends JsonDeserializer<Integer> {
        @Override
        public Integer deserialize(JsonParser parser, DeserializationContext context) throws IOException {
            try {
                return Integer.valueOf(String.valueOf(parser.readValueAs(Object.class)));
            } catch (NumberFormatException ex) {
                // Preserve "rating is required" for non-integral or malformed ratings.
                return null;
            }
        }
    }

    public static class LegacyBoolean extends JsonDeserializer<Boolean> {
        @Override
        public Boolean deserialize(JsonParser parser, DeserializationContext context) throws IOException {
            return RowMaps.asBool(parser.readValueAs(Object.class));
        }
    }

    public static class OptionalInteger extends JsonDeserializer<Integer> {
        @Override
        public Integer deserialize(JsonParser parser, DeserializationContext context) throws IOException {
            Object value = parser.readValueAs(Object.class);
            if (value instanceof Number number) return number.intValue();
            try {
                return Integer.valueOf(String.valueOf(value));
            } catch (NumberFormatException ex) {
                // Null lets the controller retain its existing default (quantity 1, duration 60).
                return null;
            }
        }
    }
}
