package com.labcompare.controller;

import com.labcompare.service.PrescriptionNotifyService;
import com.labcompare.service.RazorpayService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Two endpoints:
 *
 *  POST /api/razorpay/create-order
 *       Body : { userName, userPhone, amount }
 *       Returns: Razorpay order object + your key-id so Angular can open checkout
 *
 *  POST /api/razorpay/verify-payment
 *       Body : { razorpay_order_id, razorpay_payment_id, razorpay_signature,
 *                userName, userPhone, tests: [...], amount }
 *       Verifies HMAC-SHA256 signature → on success fires team email (async)
 *       Returns: { success, message, payment_id, order_id }
 */
@RestController
@RequestMapping("/api/razorpay")
@CrossOrigin(origins = "*")
public class RazorpayController {

    private static final Logger log = LoggerFactory.getLogger(RazorpayController.class);

    private final RazorpayService          razorpayService;
    private final PrescriptionNotifyService notifyService;

    @Value("${razorpay.key-id}")
    private String razorpayKeyId;

    public RazorpayController(RazorpayService razorpayService,
                               PrescriptionNotifyService notifyService) {
        this.razorpayService = razorpayService;
        this.notifyService   = notifyService;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // STEP 1 — Angular calls this first to get a Razorpay order id
    // ─────────────────────────────────────────────────────────────────────────
    @PostMapping("/create-order")
    public ResponseEntity<?> createOrder(@RequestBody Map<String, Object> req) {
        try {
            String userName  = (String) req.get("userName");
            String userPhone = (String) req.get("userPhone");
            Object amountObj = req.get("amount");

            if (userName  == null || userName.isBlank())
                return badRequest("userName is required");
            if (userPhone == null || userPhone.isBlank())
                return badRequest("userPhone is required");
            if (amountObj == null)
                return badRequest("amount is required");

            long amountInRupees = ((Number) amountObj).longValue();
            long amountInPaise  = amountInRupees * 100;

            // Short unique receipt visible in Razorpay dashboard
            String receipt = "RX-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
            String notes   = "Prescription review fee — " + userName + " | " + userPhone;

            Map<String, Object> order = razorpayService.createOrder(amountInPaise, receipt, notes);

            // Inject your public key so Angular doesn't need it hard-coded
            order.put("key", razorpayKeyId);

            return ResponseEntity.ok(order);

        } catch (Exception e) {
            log.error("[RazorpayController] create-order error: {}", e.getMessage());
            return serverError("Could not create payment order: " + e.getMessage());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // STEP 2 — Angular calls this after Razorpay's handler fires on success
    // ─────────────────────────────────────────────────────────────────────────
    @PostMapping("/verify-payment")
    public ResponseEntity<?> verifyPayment(@RequestBody Map<String, Object> req) {
        try {
            String orderId   = (String) req.get("razorpay_order_id");
            String paymentId = (String) req.get("razorpay_payment_id");
            String signature = (String) req.get("razorpay_signature");
            String userName  = (String) req.get("userName");
            String userPhone = (String) req.get("userPhone");
            Object amountObj = req.get("amount");

            @SuppressWarnings("unchecked")
            List<String> tests = (List<String>) req.get("tests");

            if (orderId == null || paymentId == null || signature == null)
                return badRequest("razorpay_order_id, razorpay_payment_id and razorpay_signature are required");

            // ── HMAC-SHA256 signature check (no external call) ──────────────
            boolean valid = razorpayService.verifySignature(orderId, paymentId, signature);
            if (!valid) {
                log.warn("[RazorpayController] ⚠️  Invalid signature | orderId={}", orderId);
                return ResponseEntity.status(400).body(
                    Map.of("success", false,
                           "message", "Payment verification failed. Please contact support."));
            }

            // ── Signature OK → queue team email (async, non-blocking) ───────
            long amount = amountObj != null ? ((Number) amountObj).longValue() : 0L;
            notifyService.sendPaymentSuccessToTeam(
                    userName, userPhone, tests, amount, orderId, paymentId);

            log.info("[RazorpayController] ✅ Verified & email queued | orderId={} paymentId={}",
                    orderId, paymentId);

            return ResponseEntity.ok(Map.of(
                "success",    true,
                "message",    "Payment verified. Our team will contact you shortly.",
                "payment_id", paymentId,
                "order_id",   orderId
            ));

        } catch (Exception e) {
            log.error("[RazorpayController] verify-payment error: {}", e.getMessage());
            return serverError("Verification error: " + e.getMessage());
        }
    }

    // ─── helpers ─────────────────────────────────────────────────────────────
    private ResponseEntity<?> badRequest(String msg) {
        return ResponseEntity.badRequest().body(Map.of("success", false, "message", msg));
    }
    private ResponseEntity<?> serverError(String msg) {
        return ResponseEntity.status(500).body(Map.of("success", false, "message", msg));
    }
}