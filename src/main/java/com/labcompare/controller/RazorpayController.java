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
 * POST /api/razorpay/create-order
 *      Body: { userName, userPhone, amount }
 *      Returns: Razorpay order object + key-id
 *
 * POST /api/razorpay/verify-payment
 *      Body: { razorpay_order_id, razorpay_payment_id, razorpay_signature,
 *              userName, userPhone, amount,
 *              -- Prescription flow --
 *              tests[], imageBase64, imageMime, imageName,
 *              -- Booking flow (all optional) --
 *              bookingRef, labName, appointmentDate, appointmentSlot,
 *              collectionType, collectionAddress }
 *
 *      → Verifies HMAC-SHA256 signature
 *      → On success fires ONE team email with all details + image attachment
 *        (image only present for prescription flow)
 */
@RestController
@RequestMapping("/api/razorpay")
@CrossOrigin(origins = "*")
public class RazorpayController {

    private static final Logger log = LoggerFactory.getLogger(RazorpayController.class);

    private final RazorpayService           razorpayService;
    private final PrescriptionNotifyService notifyService;

    @Value("${razorpay.key-id}")
    private String razorpayKeyId;

    public RazorpayController(RazorpayService razorpayService,
                               PrescriptionNotifyService notifyService) {
        this.razorpayService = razorpayService;
        this.notifyService   = notifyService;
    }

    // ── STEP 1: Create Razorpay order ─────────────────────────────────────────
    @PostMapping("/create-order")
    public ResponseEntity<?> createOrder(@RequestBody Map<String, Object> req) {
        try {
            String userName  = (String) req.get("userName");
            String userPhone = (String) req.get("userPhone");
            Object amountObj = req.get("amount");

            if (userName  == null || userName.isBlank())  return badRequest("userName is required");
            if (userPhone == null || userPhone.isBlank()) return badRequest("userPhone is required");
            if (amountObj == null)                        return badRequest("amount is required");

            long amountInPaise = ((Number) amountObj).longValue() * 100;
            String receipt     = "RX-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
            String notes       = "LabChain payment — " + userName + " | " + userPhone;

            Map<String, Object> order = razorpayService.createOrder(amountInPaise, receipt, notes);
            order.put("key", razorpayKeyId);
            return ResponseEntity.ok(order);

        } catch (Exception e) {
            log.error("[Razorpay] create-order error: {}", e.getMessage());
            return serverError("Could not create payment order: " + e.getMessage());
        }
    }

    // ── STEP 2: Verify payment + send ONE team email ───────────────────────────
    @PostMapping("/verify-payment")
    public ResponseEntity<?> verifyPayment(@RequestBody Map<String, Object> req) {
        try {
            // ── Core payment fields ──────────────────────────────────────────
            String orderId   = (String) req.get("razorpay_order_id");
            String paymentId = (String) req.get("razorpay_payment_id");
            String signature = (String) req.get("razorpay_signature");
            String userName  = (String) req.get("userName");
            String userPhone = (String) req.get("userPhone");
            Object amountObj = req.get("amount");

            if (orderId == null || paymentId == null || signature == null)
                return badRequest("razorpay_order_id, razorpay_payment_id, razorpay_signature are required");

            // ── Prescription-flow fields (null for booking flow) ─────────────
            @SuppressWarnings("unchecked")
            List<String> tests = (List<String>) req.get("tests");
            String imageBase64 = (String) req.get("imageBase64");   // base64 string, no prefix
            String imageMime   = (String) req.get("imageMime");     // e.g. "image/jpeg"
            String imageName   = (String) req.get("imageName");     // e.g. "prescription.jpg"

            // ── Booking-flow fields (null for prescription flow) ─────────────
            String bookingRef        = (String) req.get("bookingRef");
            String labName           = (String) req.get("labName");
            String appointmentDate   = (String) req.get("appointmentDate");
            String appointmentSlot   = (String) req.get("appointmentSlot");
            String collectionType    = (String) req.get("collectionType");
            String collectionAddress = (String) req.get("collectionAddress");

            // ── HMAC-SHA256 verification (offline, no external call) ─────────
            boolean valid = razorpayService.verifySignature(orderId, paymentId, signature);
            if (!valid) {
                log.warn("[Razorpay] ⚠️  Signature mismatch | orderId={}", orderId);
                return ResponseEntity.status(400).body(
                    Map.of("success", false, "message", "Payment verification failed. Please contact support."));
            }

            // ── ONE email, async, non-blocking ───────────────────────────────
            long amount = amountObj != null ? ((Number) amountObj).longValue() : 0L;

            notifyService.sendPaymentSuccessToTeam(
                    userName, userPhone, tests, amount,
                    orderId, paymentId,
                    imageBase64, imageMime, imageName,
                    bookingRef, labName,
                    appointmentDate, appointmentSlot,
                    collectionType, collectionAddress);

            log.info("[Razorpay] ✅ Verified & email queued | orderId={} paymentId={}", orderId, paymentId);

            return ResponseEntity.ok(Map.of(
                "success",    true,
                "message",    "Payment verified. Our team will contact you shortly.",
                "payment_id", paymentId,
                "order_id",   orderId
            ));

        } catch (Exception e) {
            log.error("[Razorpay] verify-payment error: {}", e.getMessage());
            return serverError("Verification error: " + e.getMessage());
        }
    }

    private ResponseEntity<?> badRequest(String msg) {
        return ResponseEntity.badRequest().body(Map.of("success", false, "message", msg));
    }
    private ResponseEntity<?> serverError(String msg) {
        return ResponseEntity.status(500).body(Map.of("success", false, "message", msg));
    }
}