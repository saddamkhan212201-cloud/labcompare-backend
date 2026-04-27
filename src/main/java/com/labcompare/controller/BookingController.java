package com.labcompare.controller;

import com.labcompare.dto.*;
import com.labcompare.service.BookingService;
import com.labcompare.service.QRCodeService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/bookings")
@CrossOrigin(origins = "*")
public class BookingController {

    private final BookingService bookingService;
    private final QRCodeService qrCodeService;

    public BookingController(BookingService bookingService, QRCodeService qrCodeService) {
        this.bookingService = bookingService;
        this.qrCodeService = qrCodeService;
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
    public ResponseEntity<ApiResponse<List<BookingDTO>>> getAll(@RequestParam(required = false) String phone) {
        if (phone != null) return ResponseEntity.ok(ApiResponse.ok(bookingService.getByPhone(phone)));
        return ResponseEntity.ok(ApiResponse.ok(bookingService.getAllBookings()));
    }

    @PatchMapping("/{ref}/cancel")
    public ResponseEntity<ApiResponse<BookingDTO>> cancel(@PathVariable String ref) {
        return ResponseEntity.ok(ApiResponse.ok("Booking cancelled", bookingService.cancelBooking(ref)));
    }
}
