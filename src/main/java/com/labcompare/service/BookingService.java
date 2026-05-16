package com.labcompare.service;

import com.labcompare.dto.*;
import com.labcompare.model.*;
import com.labcompare.repository.*;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
public class BookingService {

    private final BookingRepository       bookingRepository;
    private final LabRepository           labRepository;
    private final TestRepository          testRepository;
    private final LabTestPriceRepository  priceRepository;
    private final EmailService            emailService;

    public BookingService(BookingRepository bookingRepository, LabRepository labRepository,
                          TestRepository testRepository, LabTestPriceRepository priceRepository,
                          EmailService emailService) {
        this.bookingRepository = bookingRepository;
        this.labRepository     = labRepository;
        this.testRepository    = testRepository;
        this.priceRepository   = priceRepository;
        this.emailService      = emailService;
    }

    /**
     * Creates and persists the booking record.
     *
     * NOTE: No email is sent here intentionally.
     * The confirmation email is sent ONLY after Razorpay payment is verified,
     * via RazorpayController → PrescriptionNotifyService.sendPaymentSuccessToTeam().
     * For CASH bookings the frontend should call /api/razorpay/confirm-cash-booking
     * or you can re-enable the email below only for CASH if needed.
     */
    public BookingDTO createBooking(BookingRequest req) {
        Lab lab   = labRepository.findById(req.getLabId())
                .orElseThrow(() -> new EntityNotFoundException("Lab not found"));
        Test test = testRepository.findById(req.getTestId())
                .orElseThrow(() -> new EntityNotFoundException("Test not found"));
        LabTestPrice price = priceRepository.findByLabIdAndTestId(req.getLabId(), req.getTestId())
                .orElseThrow(() -> new EntityNotFoundException("Price not configured for this lab and test"));

        double testPrice     = price.getEffectivePrice();
        double collectionFee = req.getCollectionType() == Booking.CollectionType.HOME ? 50.0 : 0.0;
        String ref           = "LC" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        Booking booking = new Booking();
        booking.setBookingRef(ref);
        booking.setPatientName(req.getPatientName());
        booking.setPatientAge(req.getPatientAge());
        booking.setPhone(req.getPhone());
        booking.setEmail(req.getEmail());
        booking.setLab(lab);
        booking.setTest(test);
        booking.setTestPrice(testPrice);
        booking.setCollectionFee(collectionFee);
        booking.setTotalAmount(testPrice + collectionFee);
        booking.setCollectionType(req.getCollectionType());
        booking.setCollectionAddress(req.getCollectionAddress());
        booking.setAppointmentDate(req.getAppointmentDate());
        booking.setAppointmentSlot(req.getAppointmentSlot());
        booking.setPaymentMethod(req.getPaymentMethod());
        booking.setStatus(Booking.BookingStatus.CONFIRMED);

        // ── Save and return DTO ──────────────────────────────────────────────
        // EMAIL IS NOT SENT HERE — it fires after payment verification only.
        return toDTO(bookingRepository.save(booking));
    }

    public BookingDTO getByRef(String ref) {
        return toDTO(bookingRepository.findByBookingRef(ref)
                .orElseThrow(() -> new EntityNotFoundException("Booking not found: " + ref)));
    }

    public List<BookingDTO> getAllBookings() {
        return bookingRepository.findAll().stream().map(this::toDTO).collect(Collectors.toList());
    }

    public List<BookingDTO> getByPhone(String phone) {
        return bookingRepository.findByPhone(phone).stream().map(this::toDTO).collect(Collectors.toList());
    }

    public BookingDTO cancelBooking(String ref) {
        Booking b = bookingRepository.findByBookingRef(ref)
                .orElseThrow(() -> new EntityNotFoundException("Booking not found: " + ref));
        b.setStatus(Booking.BookingStatus.CANCELLED);
        BookingDTO dto = toDTO(bookingRepository.save(b));
        try { emailService.sendCancellationEmail(dto); } catch (Exception ignored) {}
        return dto;
    }

    private BookingDTO toDTO(Booking b) {
        BookingDTO dto = new BookingDTO();
        dto.setId(b.getId());
        dto.setBookingRef(b.getBookingRef());
        dto.setPatientName(b.getPatientName());
        dto.setPatientAge(b.getPatientAge());
        dto.setPhone(b.getPhone());
        dto.setEmail(b.getEmail());
        dto.setLabName(b.getLab().getName());
        dto.setTestName(b.getTest().getName());
        dto.setTestPrice(b.getTestPrice());
        dto.setCollectionFee(b.getCollectionFee());
        dto.setTotalAmount(b.getTotalAmount());
        dto.setCollectionType(b.getCollectionType().name());
        dto.setCollectionAddress(b.getCollectionAddress());
        dto.setAppointmentDate(b.getAppointmentDate());
        dto.setAppointmentSlot(b.getAppointmentSlot());
        dto.setPaymentMethod(b.getPaymentMethod().name());
        dto.setStatus(b.getStatus().name());
        dto.setCreatedAt(b.getCreatedAt());
        return dto;
    }
}