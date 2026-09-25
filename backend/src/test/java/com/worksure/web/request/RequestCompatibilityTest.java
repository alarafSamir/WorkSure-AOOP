package com.worksure.web.request;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.worksure.util.RowMaps;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class RequestCompatibilityTest {
    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void fractionalIdentifiersCannotSilentlyTargetAnotherRecord() throws Exception {
        assertEquals(9L, mapper.readValue("{\"service_id\":9.0}", CreateBookingRequest.class).getServiceId());
        assertThrows(com.fasterxml.jackson.core.JsonProcessingException.class,
                () -> mapper.readValue("{\"service_id\":9.5}", CreateBookingRequest.class));
    }

    @Test
    void snakeCaseFieldsAndNumericStringsStillBind() throws Exception {
        RegisterRequest registration = mapper.readValue("""
                {"email":"test@example.com","password":"password123","full_name":"Test User","role":"worker"}
                """, RegisterRequest.class);
        assertEquals("Test User", registration.getFullName());
        CreateServiceRequest service = mapper.readValue("""
                {"category_id":"2","base_price":"125.50","duration_minutes":90,"title":"Cleaning"}
                """, CreateServiceRequest.class);
        assertEquals(2L, service.getCategoryId());
        assertEquals(new BigDecimal("125.50"), service.getBasePrice());
        assertEquals(90, service.getDurationMinutes());
        CreateBookingRequest booking = mapper.readValue("""
                {"service_id":"9","scheduled_at":"2030-01-01T12:00:00Z","address":"Dhaka"}
                """, CreateBookingRequest.class);
        assertEquals(9L, booking.getServiceId());
        assertEquals("2030-01-01T12:00:00Z", booking.getScheduledAt());
    }

    @Test
    void absentAndExplicitNullAreDifferentInPartialUpdates() throws Exception {
        UpdateProfileRequest absent = mapper.readValue("{}", UpdateProfileRequest.class);
        UpdateProfileRequest clear = mapper.readValue("{\"phone\":null}", UpdateProfileRequest.class);
        assertFalse(absent.hasPhone());
        assertTrue(clear.hasPhone());
        assertNull(clear.getPhone());
        assertFalse(clear.hasFullName());
        UpdateServiceRequest service = mapper.readValue("{\"tags\":null,\"is_active\":false}", UpdateServiceRequest.class);
        assertTrue(service.hasTags());
        assertNull(service.getTags());
        assertTrue(service.hasIsActive());
        assertFalse(service.getIsActive());
        assertFalse(service.hasBasePrice());
        UpdateWorkerProfileRequest worker = mapper.readValue("{\"availability\":null}", UpdateWorkerProfileRequest.class);
        assertTrue(worker.hasAvailability());
        assertNull(RequestJson.databaseValue(worker.getAvailability()));
        assertFalse(worker.hasHourlyRate());
    }

    @Test
    void missingDefaultsAndExplicitNullMatchOldMapBehavior() throws Exception {
        assertEquals("", mapper.readValue("{}", CreateBookingRequest.class).getAddress());
        assertNull(mapper.readValue("{\"address\":null}", CreateBookingRequest.class).getAddress());
        assertEquals("success", mapper.readValue("{}", MockPaymentRequest.class).getSimulate());
        assertNull(mapper.readValue("{\"simulate\":null}", MockPaymentRequest.class).getSimulate());
    }

    @Test
    void defaultedIntegersKeepTheirPreviousFallbacks() throws Exception {
        for (String value : new String[]{"null", "\"invalid\"", "true", "{}", "[]"}) {
            CreateServiceRequest service = mapper.readValue("{\"duration_minutes\":" + value + "}", CreateServiceRequest.class);
            assertEquals(60, RowMaps.asInt(service.getDurationMinutes(), 60));
            AddCartItemRequest cart = mapper.readValue("{\"quantity\":" + value + "}", AddCartItemRequest.class);
            assertEquals(1, RowMaps.asInt(cart.getQuantity(), 1));
        }
        assertEquals(2, mapper.readValue("{\"quantity\":2.9}", AddCartItemRequest.class).getQuantity());
    }

    @Test
    void flexibleBooleanValuesStillMatchRowMaps() throws Exception {
        for (String json : new String[]{"true", "false", "1", "0", "2", "\"TRUE\"", "\"1\"", "\"yes\"", "null"}) {
            boolean expected = RowMaps.asBool(mapper.readValue(json, Object.class));
            UpdateUserBanRequest ban = mapper.readValue("{\"is_banned\":" + json + "}", UpdateUserBanRequest.class);
            UpdateServiceRequest service = mapper.readValue("{\"is_active\":" + json + "}", UpdateServiceRequest.class);
            assertEquals(expected, RowMaps.asBool(ban.getIsBanned()));
            assertEquals(expected, RowMaps.asBool(service.getIsActive()));
        }
    }

    @Test
    void ratingParsingDoesNotSilentlyTruncateFractions() throws Exception {
        assertEquals(5, mapper.readValue("{\"rating\":5}", CreateReviewRequest.class).getRating());
        assertEquals(5, mapper.readValue("{\"rating\":\"5\"}", CreateReviewRequest.class).getRating());
        for (String value : new String[]{"4.5", "4.0", "\"bad\"", "null", "true", "{}"}) {
            assertNull(mapper.readValue("{\"rating\":" + value + "}", CreateReviewRequest.class).getRating());
        }
    }

    @Test
    void jsonColumnsKeepBothStructuredAndEncodedJsonSupport() throws Exception {
        for (String value : new String[]{"{\"mon\":true}", "[\"/images/test.jpg\"]", "null", "\"{\\\"mon\\\":true}\""}) {
            assertEquals(RowMaps.json(mapper.readValue(value, Object.class)), RequestJson.databaseValue(mapper.readTree(value)));
        }
    }

    @Test
    void unsupportedFieldsCannotBecomeEditable() throws Exception {
        UpdateServiceRequest service = mapper.readValue("{\"worker_id\":999,\"category_id\":99}", UpdateServiceRequest.class);
        assertFalse(service.hasTitle());
        assertFalse(service.hasIsActive());
        UpdateProfileRequest user = mapper.readValue("{\"role\":\"admin\",\"is_banned\":false}", UpdateProfileRequest.class);
        assertFalse(user.hasFullName());
    }
}
