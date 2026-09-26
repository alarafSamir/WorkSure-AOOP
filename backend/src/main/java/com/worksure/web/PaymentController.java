package com.worksure.web;

import com.worksure.web.request.MockPaymentRequest;
import com.worksure.web.request.CreatePaymentIntentRequest;
import com.worksure.web.request.ConfirmPaymentRequest;

import com.stripe.Stripe;
import com.stripe.model.Event;
import com.stripe.model.PaymentIntent;
import com.stripe.net.Webhook;
import com.stripe.param.PaymentIntentCreateParams;
import com.worksure.db.Db;
import com.worksure.security.AuthUser;
import com.worksure.security.SecurityUtils;
import com.worksure.socket.RealtimeService;
import com.worksure.util.Commissions;
import com.worksure.util.Invoices;
import com.worksure.util.RowMaps;
import com.worksure.util.StripeAmounts;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {
    private final Db db;
    private final RealtimeService realtime;
    private final Commissions commissions;
    private final StripeAmounts stripeAmounts;
    private final String stripeSecret;
    private final String stripePublishable;
    private final String webhookSecret;
    private final String stripeCurrency;

    public PaymentController(
            Db db,
            RealtimeService realtime,
            Commissions commissions,
            StripeAmounts stripeAmounts,
            @Value("${stripe.secret-key:}") String stripeSecret,
            @Value("${stripe.publishable-key:}") String stripePublishable,
            @Value("${stripe.webhook-secret:}") String webhookSecret,
            @Value("${stripe.currency:usd}") String stripeCurrency
    ) {
        this.db = db;
        this.realtime = realtime;
        this.commissions = commissions;
        this.stripeAmounts = stripeAmounts;
        this.stripeSecret = stripeSecret;
        this.stripePublishable = stripePublishable;
        this.webhookSecret = webhookSecret;
        this.stripeCurrency = stripeCurrency;
        if (stripeSecret != null && !stripeSecret.isBlank()) {
            Stripe.apiKey = stripeSecret;
        }
    }

    @GetMapping("/stripe/config")
    public Map<String, Object> stripeConfig() {
        Map<String, Object> res = ApiResponses.ok();
        res.put("enabled", stripeEnabled() && stripePublishable != null && !stripePublishable.isBlank());
        res.put("publishableKey", stripePublishable == null ? "" : stripePublishable);
        res.put("commissionRate", commissions.rate());
        res.put("currency", stripeCurrency);
        return res;
    }

    @PostMapping("/mock")
    public ResponseEntity<Map<String, Object>> mockPay(@RequestBody MockPaymentRequest body) {
        AuthUser auth = SecurityUtils.currentUser();
        Long bookingId = body.getBookingId();
        String provider = String.valueOf(body.getProvider());
        if (!List.of("bkash", "nagad", "mock_card").contains(provider)) {
            throw new ApiException(400, "Invalid provider");
        }
        Map<String, Object> booking = assertPayable(bookingId, auth);
        Map<String, Object> split = commissions.split(booking.get("total_price"));
        String simulate = String.valueOf(body.getSimulate());
        String status = "failure".equals(simulate) ? "failed" : "completed";
        String trx = "MOCK-" + provider.toUpperCase() + "-" + System.currentTimeMillis();
        long id = db.insert(
                """
                INSERT INTO payments (booking_id, payer_id, amount, platform_commission, worker_payout, provider, status, transaction_ref, invoice_number, meta)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                bookingId, auth.id(), split.get("amount"), split.get("platform_commission"), split.get("worker_payout"),
                provider, status, trx, Invoices.generate(),
                RowMaps.json(Map.of("mock", true, "provider", provider, "commission_rate", commissions.rate()))
        );
        finalizePayment(booking, id, status);
        Map<String, Object> payment = formatPayment(db.queryOne("SELECT * FROM payments WHERE id = ?", id));
        Map<String, Object> res = ApiResponses.ok("payment", payment);
        res.put("redirectUrl", "completed".equals(status) ? "/customer/payments?highlight=" + id : null);
        return ResponseEntity.status(201).body(res);
    }

    @PostMapping("/stripe/create-intent")
    public Map<String, Object> createIntent(@RequestBody CreatePaymentIntentRequest body) {
        if (!stripeEnabled()) {
            throw new ApiException(503, "Stripe is not configured. Set stripe.secret-key in application.properties");
        }
        AuthUser auth = SecurityUtils.currentUser();
        Long bookingId = body.getBookingId();
        Map<String, Object> booking = assertPayable(bookingId, auth);
        Map<String, Object> split = commissions.split(booking.get("total_price"));
        Map<String, Object> charge = stripeAmounts.toCharge(split.get("amount"));

        Map<String, Object> pending = db.queryOne(
                """
                SELECT * FROM payments WHERE booking_id = ? AND provider = 'stripe' AND status = 'pending'
                AND stripe_payment_intent_id IS NOT NULL ORDER BY id DESC LIMIT 1
                """,
                bookingId
        );
        try {
            if (pending != null && pending.get("stripe_payment_intent_id") != null) {
                PaymentIntent existing = PaymentIntent.retrieve(String.valueOf(pending.get("stripe_payment_intent_id")));
                if ("requires_payment_method".equals(existing.getStatus()) || "requires_confirmation".equals(existing.getStatus())) {
                    return intentResponse(existing, split, charge);
                }
                if ("succeeded".equals(existing.getStatus())) {
                    markStripeCompleted(existing);
                    throw new ApiException(400, "Booking already paid");
                }
            }
            PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
                    .setAmount((Long) charge.get("unitAmount"))
                    .setCurrency(String.valueOf(charge.get("currency")))
                    .putMetadata("booking_id", String.valueOf(bookingId))
                    .putMetadata("payer_id", String.valueOf(auth.id()))
                    .putMetadata("platform_commission", String.valueOf(split.get("platform_commission")))
                    .putMetadata("worker_payout", String.valueOf(split.get("worker_payout")))
                    .putMetadata("amount_bdt", String.valueOf(split.get("amount")))
                    .setAutomaticPaymentMethods(
                            PaymentIntentCreateParams.AutomaticPaymentMethods.builder().setEnabled(true).build()
                    )
                    .build();
            PaymentIntent intent = PaymentIntent.create(params);
            if (pending != null) {
                db.run(
                        "UPDATE payments SET amount = ?, platform_commission = ?, worker_payout = ?, stripe_payment_intent_id = ?, meta = ? WHERE id = ?",
                        split.get("amount"), split.get("platform_commission"), split.get("worker_payout"),
                        intent.getId(), RowMaps.json(Map.of("stripe_status", intent.getStatus(), "stripe_currency", charge.get("currency"))),
                        pending.get("id")
                );
            } else {
                db.insert(
                        """
                        INSERT INTO payments (booking_id, payer_id, amount, platform_commission, worker_payout, provider, status, stripe_payment_intent_id, invoice_number, meta)
                        VALUES (?, ?, ?, ?, ?, 'stripe', 'pending', ?, ?, ?)
                        """,
                        bookingId, auth.id(), split.get("amount"), split.get("platform_commission"), split.get("worker_payout"),
                        intent.getId(), Invoices.generate(),
                        RowMaps.json(Map.of("stripe_status", intent.getStatus(), "stripe_currency", charge.get("currency")))
                );
            }
            return intentResponse(intent, split, charge);
        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            throw new ApiException(400, e.getMessage() != null ? e.getMessage() : "Stripe could not create payment");
        }
    }

    @PostMapping("/stripe/confirm")
    public Map<String, Object> confirm(@RequestBody ConfirmPaymentRequest body) {
        if (!stripeEnabled()) {
            throw new ApiException(503, "Stripe not configured");
        }
        AuthUser auth = SecurityUtils.currentUser();
        Long bookingId = body.getBookingId();
        String pi = String.valueOf(body.getPaymentIntentId());
        try {
            PaymentIntent intent = PaymentIntent.retrieve(pi);
            if (intent.getMetadata() != null && intent.getMetadata().get("booking_id") != null
                    && !String.valueOf(bookingId).equals(intent.getMetadata().get("booking_id"))) {
                throw new ApiException(400, "Payment intent does not match booking");
            }
            if (!"succeeded".equals(intent.getStatus())) {
                db.run("UPDATE payments SET status = 'failed', meta = ? WHERE stripe_payment_intent_id = ?",
                        RowMaps.json(Map.of("stripe_status", intent.getStatus())), pi);
                throw new ApiException(400, "Payment not completed: " + intent.getStatus());
            }
            Map<String, Object> paid = db.queryOne("SELECT * FROM payments WHERE booking_id = ? AND status = 'completed'", bookingId);
            if (paid != null) {
                return ApiResponses.ok("payment", formatPayment(paid));
            }
            assertPayable(bookingId, auth);
            Map<String, Object> payment = markStripeCompleted(intent);
            if (payment == null) {
                throw new ApiException(404, "Payment record not found");
            }
            return ApiResponses.ok("payment", formatPayment(payment));
        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            throw new ApiException(400, e.getMessage());
        }
    }

    @PostMapping(value = "/stripe/webhook", consumes = "application/json")
    // Signature verification requires the original bytes, not a deserialized request DTO.
    public ResponseEntity<Map<String, Object>> webhook(
            @RequestHeader(value = "Stripe-Signature", required = false) String sig,
            @RequestBody byte[] payload
    ) {
        if (stripeSecret == null || stripeSecret.isBlank() || webhookSecret == null || webhookSecret.isBlank()) {
            return ResponseEntity.status(503).body(ApiResponses.fail("Webhook not configured"));
        }
        try {
            Event event = Webhook.constructEvent(new String(payload), sig, webhookSecret);
            if ("payment_intent.succeeded".equals(event.getType())) {
                PaymentIntent intent = (PaymentIntent) event.getDataObjectDeserializer().getObject().orElse(null);
                if (intent != null) {
                    markStripeCompleted(intent);
                }
            }
            return ResponseEntity.ok(Map.of("received", true));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponses.fail("Webhook Error: " + e.getMessage()));
        }
    }

    @GetMapping("/mine")
    public Map<String, Object> mine() {
        AuthUser auth = SecurityUtils.currentUser();
        List<Map<String, Object>> rows = db.query(
                """
                SELECT p.*, s.title AS service_title, b.status AS booking_status
                FROM payments p
                JOIN bookings b ON b.id = p.booking_id
                JOIN services s ON s.id = b.service_id
                WHERE p.payer_id = ? ORDER BY p.id DESC
                """,
                auth.id()
        );
        return ApiResponses.ok("payments", rows.stream().map(this::formatPayment).toList());
    }

    @GetMapping("/invoice/{id}")
    public Map<String, Object> invoice(@PathVariable long id) {
        AuthUser auth = SecurityUtils.currentUser();
        Map<String, Object> payment = db.queryOne(
                """
                SELECT p.*, b.scheduled_at, b.address, u.full_name AS payer_name, s.title AS service_title,
                       wk.user_id AS worker_user_id, wu.full_name AS worker_name
                FROM payments p
                JOIN bookings b ON b.id = p.booking_id
                JOIN users u ON u.id = p.payer_id
                JOIN services s ON s.id = b.service_id
                JOIN workers wk ON wk.id = b.worker_id
                JOIN users wu ON wu.id = wk.user_id
                WHERE p.id = ?
                """,
                id
        );
        if (payment == null) {
            throw new ApiException(404, "Not found");
        }
        if (RowMaps.asLong(payment.get("payer_id")) != auth.id() && !"admin".equals(auth.role())) {
            throw new ApiException(403, "Forbidden");
        }
        Map<String, Object> invoice = formatPayment(payment);
        invoice.put("line_items", List.of(
                Map.of("label", "Service total (BDT)", "amount", payment.get("amount")),
                Map.of("label", "Platform commission (20%)", "amount", payment.get("platform_commission")),
                Map.of("label", "Worker payout (80%)", "amount", payment.get("worker_payout"))
        ));
        return ApiResponses.ok("invoice", invoice);
    }

    private Map<String, Object> assertPayable(Object bookingId, AuthUser auth) {
        Map<String, Object> booking = db.queryOne(
                "SELECT b.*, s.title FROM bookings b JOIN services s ON s.id = b.service_id WHERE b.id = ?",
                bookingId
        );
        if (booking == null) {
            throw new ApiException(404, "Booking not found");
        }
        if (RowMaps.asLong(booking.get("customer_id")) != auth.id() && !"admin".equals(auth.role())) {
            throw new ApiException(403, "Forbidden");
        }
        String st = String.valueOf(booking.get("status"));
        if ("cancelled".equals(st) || "rejected".equals(st)) {
            throw new ApiException(400, "Booking cannot be paid");
        }
        if (db.queryOne("SELECT id FROM payments WHERE booking_id = ? AND status = 'completed'", bookingId) != null) {
            throw new ApiException(400, "Booking already paid");
        }
        return booking;
    }

    private void finalizePayment(Map<String, Object> booking, long paymentId, String status) {
        long customerId = RowMaps.asLong(booking.get("customer_id"));
        if (!"completed".equals(status)) {
            realtime.notifyUser(customerId, "payment", "Payment failed",
                    "Your payment could not be processed. Please try again.", Map.of("booking_id", booking.get("id")));
            return;
        }
        realtime.notifyUser(customerId, "payment", "Payment successful",
                "Paid ৳" + booking.get("total_price") + " for " + booking.get("title"),
                Map.of("payment_id", paymentId, "booking_id", booking.get("id")));
        Map<String, Object> worker = db.queryOne("SELECT user_id FROM workers WHERE id = ?", booking.get("worker_id"));
        if (worker != null && worker.get("user_id") != null) {
            realtime.notifyUser(RowMaps.asLong(worker.get("user_id")), "payment", "Payment received",
                    "Customer paid for booking #" + booking.get("id") + ". Your payout will be processed after commission.",
                    Map.of("payment_id", paymentId, "booking_id", booking.get("id")));
        }
    }

    private Map<String, Object> markStripeCompleted(PaymentIntent intent) {
        String bookingId = intent.getMetadata() != null ? intent.getMetadata().get("booking_id") : null;
        if (bookingId == null) {
            return null;
        }
        Map<String, Object> payment = db.queryOne(
                "SELECT * FROM payments WHERE stripe_payment_intent_id = ? OR (booking_id = ? AND provider = 'stripe' AND status = 'pending')",
                intent.getId(), bookingId
        );
        if (payment == null) {
            return null;
        }
        if ("completed".equals(String.valueOf(payment.get("status")))) {
            return payment;
        }
        db.run(
                "UPDATE payments SET status = 'completed', transaction_ref = ?, stripe_payment_intent_id = ?, meta = ? WHERE id = ?",
                intent.getId(), intent.getId(),
                RowMaps.json(Map.of("stripe_status", intent.getStatus(), "charge", String.valueOf(intent.getLatestCharge()))),
                payment.get("id")
        );
        Map<String, Object> booking = db.queryOne(
                "SELECT b.*, s.title FROM bookings b JOIN services s ON s.id = b.service_id WHERE b.id = ?",
                bookingId
        );
        if (booking != null) {
            finalizePayment(booking, RowMaps.asLong(payment.get("id")), "completed");
        }
        return db.queryOne("SELECT * FROM payments WHERE id = ?", payment.get("id"));
    }

    private Map<String, Object> intentResponse(PaymentIntent intent, Map<String, Object> split, Map<String, Object> charge) {
        Map<String, Object> res = ApiResponses.ok();
        res.put("clientSecret", intent.getClientSecret());
        res.put("publishableKey", stripePublishable == null ? "" : stripePublishable);
        res.put("amount", split.get("amount"));
        res.put("platform_commission", split.get("platform_commission"));
        res.put("worker_payout", split.get("worker_payout"));
        res.put("stripe_currency", charge.get("currency"));
        res.put("stripe_display_note", charge.get("displayNote"));
        return res;
    }

    private Map<String, Object> formatPayment(Map<String, Object> p) {
        if (p == null) {
            return null;
        }
        Map<String, Object> copy = new LinkedHashMap<>(p);
        copy.put("status_label", "completed".equals(String.valueOf(p.get("status"))) ? "paid" : p.get("status"));
        return copy;
    }

    private boolean stripeEnabled() {
        return stripeSecret != null && !stripeSecret.isBlank() && stripePublishable != null && !stripePublishable.isBlank();
    }
}
