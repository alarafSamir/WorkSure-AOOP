package com.worksure.web;

import com.worksure.web.request.AddCartItemRequest;
import com.worksure.web.request.UpdateCartItemRequest;
import com.worksure.web.request.CheckoutRequest;

import com.worksure.db.Db;
import com.worksure.security.AuthUser;
import com.worksure.security.SecurityUtils;
import com.worksure.socket.RealtimeService;
import com.worksure.util.DateTimes;
import com.worksure.util.RowMaps;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/cart")
public class CartController {
    private final Db db;
    private final RealtimeService realtime;

    public CartController(Db db, RealtimeService realtime) {
        this.db = db;
        this.realtime = realtime;
    }

    @GetMapping
    public Map<String, Object> get() {
        AuthUser auth = requireCustomer();
        long cartId = getOrCreateCart(auth.id());
        List<Map<String, Object>> items = db.query(
                """
                SELECT ci.*, s.title, s.base_price, s.duration_minutes, u.full_name AS worker_name, w.is_verified
                FROM cart_items ci
                JOIN services s ON s.id = ci.service_id
                JOIN workers w ON w.id = s.worker_id
                JOIN users u ON u.id = w.user_id
                WHERE ci.cart_id = ?
                """,
                cartId
        );
        double subtotal = 0;
        for (Map<String, Object> i : items) {
            BigDecimal price = new BigDecimal(String.valueOf(i.get("base_price")));
            int qty = RowMaps.asInt(i.get("quantity"), 1);
            subtotal += price.doubleValue() * qty;
        }
        Map<String, Object> res = ApiResponses.ok("items", items);
        res.put("cart_id", cartId);
        res.put("subtotal", subtotal);
        return res;
    }

    @PostMapping("/items")
    public ResponseEntity<Map<String, Object>> add(@RequestBody AddCartItemRequest body) {
        AuthUser auth = requireCustomer();
        Long serviceId = body.getServiceId();
        if (serviceId == null) {
            throw new ApiException(400, "service_id is required");
        }
        if (db.queryOne("SELECT id FROM services WHERE id = ? AND is_active = 1", serviceId) == null) {
            throw new ApiException(404, "Service not found");
        }
        int qty = RowMaps.asInt(body.getQuantity(), 1);
        String scheduled = body.getScheduledAt() != null ? DateTimes.toMysqlDatetime(body.getScheduledAt()) : null;
        long cartId = getOrCreateCart(auth.id());
        db.run(
                """
                INSERT INTO cart_items (cart_id, service_id, quantity, scheduled_at) VALUES (?, ?, ?, ?)
                ON DUPLICATE KEY UPDATE quantity = quantity + VALUES(quantity), scheduled_at = COALESCE(VALUES(scheduled_at), scheduled_at)
                """,
                cartId, serviceId, qty, scheduled
        );
        return ResponseEntity.status(201).body(ApiResponses.ok());
    }

    @PatchMapping("/items/{itemId}")
    public Map<String, Object> update(@PathVariable long itemId, @RequestBody UpdateCartItemRequest body) {
        AuthUser auth = requireCustomer();
        long cartId = getOrCreateCart(auth.id());
        String scheduled = body.getScheduledAt() != null ? DateTimes.toMysqlDatetime(body.getScheduledAt()) : null;
        db.run("UPDATE cart_items SET quantity = ?, scheduled_at = ? WHERE id = ? AND cart_id = ?",
                body.getQuantity(), scheduled, itemId, cartId);
        return ApiResponses.ok();
    }

    @DeleteMapping("/items/{itemId}")
    public Map<String, Object> remove(@PathVariable long itemId) {
        AuthUser auth = requireCustomer();
        long cartId = getOrCreateCart(auth.id());
        db.run("DELETE FROM cart_items WHERE id = ? AND cart_id = ?", itemId, cartId);
        return ApiResponses.ok();
    }

    @PostMapping("/checkout")
    public Map<String, Object> checkout(@RequestBody(required = false) CheckoutRequest body) {
        AuthUser auth = requireCustomer();
        long cartId = getOrCreateCart(auth.id());
        List<Map<String, Object>> items = db.query("SELECT * FROM cart_items WHERE cart_id = ?", cartId);
        if (items.isEmpty()) {
            throw new ApiException(400, "Cart is empty");
        }
        String address = body != null && body.getDefaultAddress() != null
                ? String.valueOf(body.getDefaultAddress())
                : "Address to be confirmed with worker";
        List<Map<String, Object>> bookings = new ArrayList<>();
        for (Map<String, Object> item : items) {
            Map<String, Object> service = db.queryOne(
                    """
                    SELECT s.*, w.id AS worker_pk, w.user_id AS worker_owner_id FROM services s
                    JOIN workers w ON w.id = s.worker_id WHERE s.id = ?
                    """,
                    item.get("service_id")
            );
            if (service == null) {
                continue;
            }
            String scheduled = item.get("scheduled_at") != null
                    ? DateTimes.toMysqlDatetime(item.get("scheduled_at"))
                    : DateTimes.defaultScheduledAt();
            BigDecimal price = new BigDecimal(String.valueOf(service.get("base_price")));
            int qty = RowMaps.asInt(item.get("quantity"), 1);
            long id = db.insert(
                    """
                    INSERT INTO bookings (customer_id, worker_id, service_id, scheduled_at, address, notes, total_price, status)
                    VALUES (?, ?, ?, ?, ?, ?, ?, 'pending')
                    """,
                    auth.id(), service.get("worker_pk"), service.get("id"), scheduled, address, "From cart checkout",
                    price.multiply(BigDecimal.valueOf(qty))
            );
            Map<String, Object> booking = db.queryOne("SELECT * FROM bookings WHERE id = ?", id);
            bookings.add(booking);
            realtime.notifyUser(RowMaps.asLong(service.get("worker_owner_id")), "booking", "New booking (cart)",
                    "Booking #" + id + " from cart", Map.of("booking_id", id));
            realtime.emitBookingUpdate(booking);
        }
        db.run("DELETE FROM cart_items WHERE cart_id = ?", cartId);
        return ApiResponses.ok("bookings", bookings);
    }

    private AuthUser requireCustomer() {
        AuthUser auth = SecurityUtils.currentUser();
        SecurityUtils.requireRole(auth, "customer", "admin");
        return auth;
    }

    private long getOrCreateCart(long userId) {
        Map<String, Object> cart = db.queryOne("SELECT id FROM carts WHERE user_id = ?", userId);
        if (cart != null) {
            return RowMaps.asLong(cart.get("id"));
        }
        return db.insert("INSERT INTO carts (user_id) VALUES (?)", userId);
    }
}
