package com.labcompare.controller;

import com.labcompare.dto.*;
import com.labcompare.service.BookingService;
import com.labcompare.service.PrescriptionNotifyService;
import com.labcompare.service.QRCodeService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/bookings")
@CrossOrigin(origins = "*")
public class BookingController {

    private final BookingService            bookingService;
    private final QRCodeService             qrCodeService;
    private final PrescriptionNotifyService notifyService;

    public BookingController(BookingService bookingService,
                             QRCodeService qrCodeService,
                             PrescriptionNotifyService notifyService) {
        this.bookingService = bookingService;
        this.qrCodeService  = qrCodeService;
        this.notifyService  = notifyService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<BookingDTO>> create(@Valid @RequestBody BookingRequest req) {
        return ResponseEntity.ok(ApiResponse.ok("Booking confirmed", bookingService.createBooking(req)));
    }

    @GetMapping("/{ref}")
    public ResponseEntity<ApiResponse<BookingDTO>> getByRef(@PathVariable String ref) {
        return ResponseEntity.ok(ApiResponse.ok(bookingService.getByRef(ref)));
    }

    @GetMapping("/{ref}/qr")
    public ResponseEntity<ApiResponse<Map<String, String>>> getQRCode(@PathVariable String ref) {
        BookingDTO booking = bookingService.getByRef(ref);
        String base64 = qrCodeService.generateBookingQR(booking);
        return ResponseEntity.ok(ApiResponse.ok(Map.of("qrBase64", base64)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<BookingDTO>>> getAll(
            @RequestParam(required = false) String phone,
            HttpServletRequest request) {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean isAdminOrSuper = auth != null && (
            auth.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_ADMIN")) ||
            auth.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_SUPERADMIN"))
        );

        if (isAdminOrSuper) {
            // Admin / SuperAdmin: can query any phone or get all bookings
            if (phone != null)
                return ResponseEntity.ok(ApiResponse.ok(bookingService.getByPhone(phone)));
            return ResponseEntity.ok(ApiResponse.ok(bookingService.getAllBookings()));
        }

        // ── Regular USER: can ONLY see their own bookings ──────────────────
        // Phone is extracted from their JWT (stored at registration) — cannot be spoofed
        String userPhone = (String) request.getAttribute("userPhone");
        if (userPhone == null || userPhone.isBlank())
            return ResponseEntity.status(403).body(
                ApiResponse.error("Your account has no phone number linked. Please contact support."));

        // Ignore any phone param from the request — always use the JWT phone
        return ResponseEntity.ok(ApiResponse.ok(bookingService.getByPhone(userPhone)));
    }

    @PatchMapping("/{ref}/cancel")
    public ResponseEntity<ApiResponse<BookingDTO>> cancel(@PathVariable String ref) {
        return ResponseEntity.ok(ApiResponse.ok("Booking cancelled", bookingService.cancelBooking(ref)));
    }

    /**
     * Called by the frontend ONCE after ALL cart items are booked (CASH flow only).
     * Sends one combined email to the patient + team regardless of cart size.
     *
     * This is intentionally separate from /api/razorpay/verify-payment so that
     * cash bookings never touch HMAC signature verification.
     */
    @PostMapping("/cash-notify")
    public ResponseEntity<?> cashNotify(@RequestBody Map<String, Object> req) {
        try {
            String userName         = (String) req.get("userName");
            String userPhone        = (String) req.get("userPhone");
            String userEmail        = (String) req.get("userEmail");
            String bookingRefs      = (String) req.get("bookingRefs");
            String labNames         = (String) req.get("labNames");
            String appointmentDate  = (String) req.get("appointmentDate");
            String appointmentSlot  = (String) req.get("appointmentSlot");
            String collectionType   = (String) req.get("collectionType");
            String collectionAddress = (String) req.get("collectionAddress");
            long   amount           = req.get("amount") != null ? ((Number) req.get("amount")).longValue() : 0L;

            @SuppressWarnings("unchecked")
            List<String> tests = (List<String>) req.get("tests");

            notifyService.sendPaymentSuccessToTeam(
                userName, userPhone, userEmail,
                tests, amount,
                "CASH-ORDER",   // orderId placeholder
                "PAY-AT-LAB",   // paymentId placeholder
                bookingRefs, labNames,
                appointmentDate, appointmentSlot,
                collectionType, collectionAddress
            );

            return ResponseEntity.ok(Map.of("success", true, "message", "Notification sent"));
        } catch (Exception e) {
            // Never fail the response — email issues should not surface to the user
            return ResponseEntity.ok(Map.of("success", false, "message", e.getMessage()));
        }
    }
}