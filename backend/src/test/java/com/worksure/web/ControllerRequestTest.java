package com.worksure.web;

import com.worksure.db.Db;
import com.worksure.security.AuthUser;
import com.worksure.security.JwtService;
import com.worksure.socket.RealtimeService;
import com.worksure.storage.UploadService;
import com.worksure.storage.VerificationDocuments;
import com.worksure.util.Categories;
import com.worksure.web.request.ReviewDocumentRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** MVC JSON-binding tests. Database/security-filter integration is a separate live check. */
class ControllerRequestTest {
    private Db db;
    private MockMvc mvc;
    private PasswordEncoder passwords;
    private JwtService jwt;

    @BeforeEach
    void setup() {
        db = mock(Db.class);
        passwords = mock(PasswordEncoder.class);
        jwt = mock(JwtService.class);
        RealtimeService realtime = mock(RealtimeService.class);
        mvc = MockMvcBuilders.standaloneSetup(
                new AuthController(db, passwords, jwt), new BookingController(db, realtime),
                new ServiceController(db, mock(Categories.class), mock(UploadService.class)),
                new UserController(db, mock(UploadService.class)),
                new WorkerController(db, mock(Categories.class), mock(VerificationDocuments.class)),
                new ReviewController(db), new AdminController(db, mock(VerificationDocuments.class)),
                new CartController(db, realtime), new ContactController())
                .setControllerAdvice(new GlobalExceptionHandler()).build();
    }

    @AfterEach
    void clearSecurity() { SecurityContextHolder.clearContext(); }

    private void login(String role, long id) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(new AuthUser(id, "test@example.com", role, "Test"), null, List.of()));
    }

    @Test
    void loginAcceptsJsonAndRetainsResponseAndInvalidCredentialsError() throws Exception {
        when(db.queryOne(contains("password_hash"), eq("test@example.com"))).thenReturn(
                new LinkedHashMap<>(Map.of("id", 7L, "role", "customer", "password_hash", "hash")));
        when(passwords.matches("correct", "hash")).thenReturn(true);
        when(jwt.sign(7L, "customer")).thenReturn("test-jwt");
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\" TEST@example.com \",\"password\":\"correct\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.token").value("test-jwt"))
                .andExpect(jsonPath("$.user.password_hash").doesNotExist());
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.message").value("Invalid credentials"));
    }

    @Test
    void registrationRetainsRequiredFieldsAndRoleRules() throws Exception {
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.message").value("Invalid email"));
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"test@example.com\",\"password\":\"password123\",\"full_name\":\"Test\",\"role\":\"admin\"}"))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.message").value("Cannot self-register as admin"));
        verifyNoInteractions(db);
    }

    @Test
    void bookingValidationAndCustomerOnlyRuleRemain() throws Exception {
        login("customer", 7);
        mvc.perform(post("/api/bookings").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("service_id, scheduled_at and address are required"));
        for (String role : List.of("worker", "admin")) {
            login(role, 7);
            mvc.perform(post("/api/bookings").contentType(MediaType.APPLICATION_JSON)
                    .content("{\"service_id\":1,\"scheduled_at\":\"2030-01-01T12:00:00Z\",\"address\":\"Dhaka\"}"))
                    .andExpect(status().isForbidden());
        }
        SecurityContextHolder.clearContext();
        mvc.perform(post("/api/bookings").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(db);
    }

    @Test
    void assignedWorkerTransitionsAndOwnershipRemain() throws Exception {
        Map<String, Object> booking = new LinkedHashMap<>(Map.of("id", 5L, "worker_id", 3L, "customer_id", 7L, "status", "pending"));
        when(db.queryOne("SELECT * FROM bookings WHERE id = ?", 5L)).thenReturn(booking);
        when(db.queryOne("SELECT id, user_id FROM workers WHERE id = ?", 3L)).thenReturn(Map.of("id", 3L, "user_id", 8L));
        when(db.run(startsWith("UPDATE bookings SET status"), any(), any(), any())).thenReturn(1);
        login("worker", 8);
        mvc.perform(patch("/api/bookings/5/status").contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"completed\"}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.message").value("Cannot change booking from pending to completed"));
        mvc.perform(patch("/api/bookings/5/status").contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"accepted\"}"))
                .andExpect(status().isOk());
        for (String role : List.of("customer", "admin", "worker")) {
            login(role, 9);
            mvc.perform(patch("/api/bookings/5/status").contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"accepted\"}"))
                    .andExpect(status().isForbidden());
        }
    }

    @Test
    void profileOnlyUpdatesSuppliedFieldsIncludingExplicitNull() throws Exception {
        login("customer", 7);
        mvc.perform(patch("/api/users/profile").contentType(MediaType.APPLICATION_JSON).content("{\"phone\":null}"))
                .andExpect(status().isOk());
        verify(db).run("UPDATE users SET phone = ? WHERE id = ?", null, 7L);
        mvc.perform(patch("/api/users/profile").contentType(MediaType.APPLICATION_JSON).content("{\"role\":\"admin\"}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.message").value("No fields to update"));
    }

    @Test
    void workerProfileJsonAndPartialUpdateRemain() throws Exception {
        login("worker", 8);
        when(db.queryOne("SELECT id FROM workers WHERE user_id = ?", 8L)).thenReturn(Map.of("id", 3L));
        mvc.perform(patch("/api/workers/me").contentType(MediaType.APPLICATION_JSON)
                .content("{\"headline\":\"Test\",\"availability\":{\"mon\":true}}"))
                .andExpect(status().isOk());
        verify(db).run("UPDATE workers SET headline = ?, availability = ? WHERE id = ?", "Test", "{\"mon\":true}", 3L);
    }

    @Test
    void servicePartialUpdateAndCreationValidationRemain() throws Exception {
        login("worker", 8);
        when(db.queryOne("SELECT id FROM workers WHERE user_id = ?", 8L)).thenReturn(Map.of("id", 3L));
        when(db.queryOne("SELECT * FROM services WHERE id = ? AND worker_id = ?", 5L, 3L)).thenReturn(Map.of("id", 5L));
        mvc.perform(patch("/api/services/5").contentType(MediaType.APPLICATION_JSON).content("{\"tags\":null,\"is_active\":\"0\"}"))
                .andExpect(status().isOk());
        verify(db).run("UPDATE services SET tags = ?, is_active = ? WHERE id = ?", null, 0, 5L);
        mvc.perform(post("/api/services").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.message").value("category_id, title, description and base_price are required"));
    }

    @Test
    void reviewRatingErrorsRemain() throws Exception {
        login("customer", 7);
        for (String value : List.of("null", "4.5", "\"bad\"")) {
            mvc.perform(post("/api/reviews").contentType(MediaType.APPLICATION_JSON).content("{\"rating\":" + value + "}"))
                    .andExpect(status().isBadRequest()).andExpect(jsonPath("$.message").value("rating is required"));
        }
        mvc.perform(post("/api/reviews").contentType(MediaType.APPLICATION_JSON).content("{\"rating\":6}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.message").value("rating must be 1-5"));
    }

    @Test
    void documentDecisionStillCountsAllApprovalsAndKeepsTransaction() throws Exception {
        login("admin", 1);
        when(db.queryOne("SELECT * FROM worker_documents WHERE id = ?", 5L)).thenReturn(Map.of("worker_id", 3L));
        when(db.queryOne(contains("COUNT(*) AS count"), eq(3L))).thenReturn(Map.of("count", 1L));
        mvc.perform(patch("/api/admin/documents/5").contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"rejected\",\"admin_note\":\"Test note\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.worker_is_verified").value(true));
        verify(db).run(startsWith("UPDATE worker_documents"), eq("rejected"), eq("Test note"), eq(1L), eq(5L));
        verify(db).run(startsWith("UPDATE workers SET verified_at"), eq(true), eq(true), eq(3L));
        Transactional tx = AdminController.class.getMethod("verifyDoc", long.class, ReviewDocumentRequest.class).getAnnotation(Transactional.class);
        assertNotNull(tx);
        assertEquals(Isolation.READ_COMMITTED, tx.isolation());
    }

    @Test
    void adminBanAndSuspendStillBindExpectedFields() throws Exception {
        login("admin", 1);
        when(db.queryOne("SELECT id, role, full_name, email FROM users WHERE id = ?", 7L))
                .thenReturn(Map.of("id", 7L, "role", "customer", "full_name", "Test"));
        mvc.perform(patch("/api/admin/users/7/ban").contentType(MediaType.APPLICATION_JSON).content("{\"is_banned\":1}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.is_banned").value(true));
        mvc.perform(patch("/api/admin/users/7/suspend").contentType(MediaType.APPLICATION_JSON).content("{\"until\":null}"))
                .andExpect(status().isOk());
        verify(db).run("UPDATE users SET suspended_until = ? WHERE id = ?", null, 7L);
    }

    @Test
    void checkoutStillAcceptsNoBody() throws Exception {
        login("customer", 7);
        when(db.queryOne("SELECT id FROM carts WHERE user_id = ?", 7L)).thenReturn(Map.of("id", 2L));
        when(db.query("SELECT * FROM cart_items WHERE cart_id = ?", 2L)).thenReturn(List.of());
        mvc.perform(post("/api/cart/checkout")).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Cart is empty"));
    }
}
