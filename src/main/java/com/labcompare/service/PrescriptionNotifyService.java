package com.labcompare.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class PrescriptionNotifyService {

    private static final Logger log    = LoggerFactory.getLogger(PrescriptionNotifyService.class);
    private static final String RESEND = "https://api.resend.com/emails";

    @Value("${resend.api-key}")
    private String resendApiKey;

    @Value("${labcompare.app.from-email}")
    private String fromEmail;

    @Value("${app.prescription.team-email}")
    private String teamEmail;

    // CC on every team email — drmedicalfoundation@gmail.com
    @Value("${app.prescription.team-cc}")
    private String teamCc;

    private final HttpClient   http   = HttpClient.newHttpClient();
    private final ObjectMapper mapper = new ObjectMapper();

    // ─────────────────────────────────────────────────────────────────────────
    // Legacy: direct prescription notify (no payment). Kept for /api/prescription/notify.
    // ─────────────────────────────────────────────────────────────────────────
    @Async
    public void sendPrescriptionToTeam(String userName, String userPhone,
                                        MultipartFile file) throws Exception {
        String timeNow  = now();
        String subject  = "New Prescription — " + userName + " | " + userPhone;
        String html     = buildPrescriptionOnlyHtml(userName, userPhone, timeNow);
        String filename = file.getOriginalFilename() != null ? file.getOriginalFilename() : "prescription.jpg";
        String b64      = Base64.getEncoder().encodeToString(file.getBytes());
        String mime     = file.getContentType() != null ? file.getContentType() : "image/jpeg";

        List<Map<String, String>> attachments = List.of(
            Map.of("filename", filename, "content", b64, "content_type", mime)
        );
        post(buildPayload("LabChain Prescriptions <" + fromEmail + ">", subject, html, attachments),
             userName, userPhone);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Called from RazorpayController after HMAC verification.
    // Sends ONE email — TO: teamEmail, CC: teamCc — with:
    //   • What the user booked / tests requested  ← clearly shown
    //   • Patient + payment details
    //   • Prescription image attached (prescription flow only)
    // ─────────────────────────────────────────────────────────────────────────
    @Async
    public void sendPaymentSuccessToTeam(
            String       userName,
            String       userPhone,
            List<String> tests,
            long         amountInRupees,
            String       razorpayOrderId,
            String       razorpayPaymentId,
            String       imageBase64,
            String       imageMime,
            String       imageName,
            String       bookingRef,
            String       labName,
            String       appointmentDate,
            String       appointmentSlot,
            String       collectionType,
            String       collectionAddress) {

        try {
            boolean isBooking = bookingRef != null && !bookingRef.isBlank();
            String  timeNow   = now();

            // Subject clearly states what was booked
            String bookedWhat = isBooking
                ? (tests != null && !tests.isEmpty() ? tests.get(0) : "Lab Test")
                : (tests != null && !tests.isEmpty() ? String.join(", ", tests) : "Prescription Review");

            String subject = isBooking
                ? "✅ Booked: " + bookedWhat + " — " + userName + " | ₹" + amountInRupees + " | Ref: " + bookingRef
                : "✅ Prescription: " + bookedWhat + " — " + userName + " | ₹" + amountInRupees;

            String html = buildPaymentHtml(
                    userName, userPhone, tests, amountInRupees,
                    razorpayOrderId, razorpayPaymentId, timeNow,
                    bookingRef, labName, appointmentDate, appointmentSlot,
                    collectionType, collectionAddress);

            // ── Attach prescription image if present (prescription flow) ────
            List<Map<String, String>> attachments = new ArrayList<>();
            if (imageBase64 != null && !imageBase64.isBlank()) {
                String mime = (imageMime != null && !imageMime.isBlank()) ? imageMime : "image/jpeg";
                String name = (imageName != null && !imageName.isBlank()) ? imageName : "prescription.jpg";
                attachments.add(Map.of("filename", name, "content", imageBase64, "content_type", mime));
            }

            post(buildPayload("LabChain Payments <" + fromEmail + ">", subject, html,
                              attachments.isEmpty() ? null : attachments),
                 userName, userPhone);

        } catch (Exception e) {
            log.error("[PrescriptionNotify] ❌ Payment email failed for {} {}: {}",
                    userName, userPhone, e.getMessage());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Internal: build Resend payload with TO + CC on every email
    // ─────────────────────────────────────────────────────────────────────────
    private Map<String, Object> buildPayload(String from, String subject,
                                              String html,
                                              List<Map<String, String>> attachments) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("from",    from);
        payload.put("to",      List.of(teamEmail));
        payload.put("cc",      List.of(teamCc));           // ← CC on every email
        payload.put("subject", subject);
        payload.put("html",    html);
        if (attachments != null && !attachments.isEmpty()) {
            payload.put("attachments", attachments);
        }
        return payload;
    }

    private void post(Map<String, Object> payload, String name, String phone) throws Exception {
        String body = mapper.writeValueAsString(payload);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(RESEND))
                .header("Authorization", "Bearer " + resendApiKey)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
        HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() == 200 || response.statusCode() == 201) {
            log.info("[PrescriptionNotify] ✅ Email sent (TO:{} CC:{}) for {} | {}", teamEmail, teamCc, name, phone);
        } else {
            log.error("[PrescriptionNotify] ❌ Resend {} | {}", response.statusCode(), response.body());
            throw new Exception("Resend error " + response.statusCode() + ": " + response.body());
        }
    }

    private String now() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a"));
    }

    private String esc(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // HTML: legacy prescription-only (no payment)
    // ─────────────────────────────────────────────────────────────────────────
    private String buildPrescriptionOnlyHtml(String userName, String userPhone, String timeNow) {
        return "<!DOCTYPE html><html><head><meta charset='UTF-8'/><style>"
            + "body{font-family:'Segoe UI',Arial,sans-serif;background:#f4f6fb;margin:0;padding:0}"
            + ".wrap{max-width:600px;margin:32px auto;background:#fff;border-radius:14px;overflow:hidden;box-shadow:0 4px 24px rgba(0,0,0,.09)}"
            + ".hdr{background:linear-gradient(135deg,#6c63ff,#a78bfa);padding:32px 40px;text-align:center}"
            + ".hdr h1{color:#fff;margin:0;font-size:22px}"
            + ".badge{display:inline-block;padding:7px 22px;border-radius:20px;font-weight:700;font-size:14px;margin-top:16px;background:#e8f5e9;color:#2e7d32}"
            + ".bdy{padding:32px 40px}"
            + ".card{background:#f8faff;border:1px solid #e3eaf7;border-radius:10px;padding:20px 24px;margin-bottom:18px}"
            + ".ct{font-size:11px;text-transform:uppercase;color:#888;letter-spacing:.8px;margin-bottom:14px;font-weight:700}"
            + ".row{display:flex;justify-content:space-between;padding:7px 0;border-bottom:1px solid #eef1f8;font-size:14px}"
            + ".row:last-child{border-bottom:none}"
            + ".row span:first-child{color:#666}.row span:last-child{color:#222;font-weight:600}"
            + ".note{background:#fff8e1;border-left:4px solid #f9a825;border-radius:0 8px 8px 0;padding:12px 16px;font-size:13px;color:#555;margin-bottom:20px;line-height:1.6}"
            + ".ftr{background:#f4f6fb;padding:18px 40px;text-align:center;font-size:12px;color:#999}"
            + "</style></head><body><div class='wrap'>"
            + "<div class='hdr'><h1>LabChain</h1><div class='badge'>📋 New Prescription Received</div></div>"
            + "<div class='bdy'>"
            + "<div class='card'><div class='ct'>Patient Details</div>"
            + "<div class='row'><span>Name</span><span>" + esc(userName) + "</span></div>"
            + "<div class='row'><span>Phone</span><span>" + esc(userPhone) + "</span></div>"
            + "<div class='row'><span>Submitted At</span><span>" + timeNow + "</span></div></div>"
            + "<div class='note'>📎 Prescription image attached. Call <strong>" + esc(userPhone) + "</strong>.</div>"
            + "</div><div class='ftr'>LabChain · Auto Notification</div></div></body></html>";
    }

    // ─────────────────────────────────────────────────────────────────────────
    // HTML: payment success — booking details OR prescription tests clearly shown
    // ─────────────────────────────────────────────────────────────────────────
    private String buildPaymentHtml(String userName, String userPhone,
                                     List<String> tests, long amount,
                                     String orderId, String paymentId, String timeNow,
                                     String bookingRef, String labName,
                                     String appointmentDate, String appointmentSlot,
                                     String collectionType, String collectionAddress) {

        boolean isBooking = bookingRef != null && !bookingRef.isBlank();

        // ── What was booked — top highlight box ──────────────────────────────
        String bookedTestName = (tests != null && !tests.isEmpty()) ? tests.get(0) : "—";
        String bookedHighlight;
        if (isBooking) {
            bookedHighlight =
                "<div style='background:linear-gradient(135deg,#e3f2fd,#e8f0fe);border:2px solid #90caf9;"
                + "border-radius:12px;padding:18px 20px;margin-bottom:20px;'>"
                + "<div style='font-size:11px;text-transform:uppercase;letter-spacing:1px;color:#1a73e8;font-weight:700;margin-bottom:6px;'>🧪 Test Booked</div>"
                + "<div style='font-size:22px;font-weight:800;color:#0d47a1;'>" + esc(bookedTestName) + "</div>"
                + (labName != null ? "<div style='font-size:13px;color:#555;margin-top:4px;'>📍 " + esc(labName) + "</div>" : "")
                + "</div>";
        } else {
            // Prescription flow — list all extracted tests
            StringBuilder testList = new StringBuilder();
            if (tests != null && !tests.isEmpty()) {
                for (int i = 0; i < tests.size(); i++) {
                    testList.append("<div style='padding:6px 0;border-bottom:1px solid #e3eaf7;font-size:14px;color:#1a1a2e;font-weight:500;'>")
                            .append(i + 1).append(". ").append(esc(tests.get(i))).append("</div>");
                }
            } else {
                testList.append("<div style='font-size:13px;color:#888;'>See prescription image attached</div>");
            }
            bookedHighlight =
                "<div style='background:linear-gradient(135deg,#e8f5e9,#f0fff4);border:2px solid #a8d5b5;"
                + "border-radius:12px;padding:18px 20px;margin-bottom:20px;'>"
                + "<div style='font-size:11px;text-transform:uppercase;letter-spacing:1px;color:#00b894;font-weight:700;margin-bottom:10px;'>🧪 Tests Requested</div>"
                + testList
                + "</div>";
        }

        // ── Booking detail rows (booking flow only) ──────────────────────────
        String bookingSection = "";
        if (isBooking) {
            StringBuilder br = new StringBuilder();
            br.append(tr("Booking Ref",  "<span style='font-family:monospace;font-weight:700;'>" + esc(bookingRef) + "</span>"));
            if (labName          != null) br.append(tr("Lab",        esc(labName)));
            if (appointmentDate  != null) br.append(tr("Date",       esc(appointmentDate)));
            if (appointmentSlot  != null) br.append(tr("Time Slot",  esc(appointmentSlot)));
            if (collectionType   != null) br.append(tr("Collection", esc(collectionType)));
            if (collectionAddress != null && !collectionAddress.isBlank())
                                          br.append(tr("Address",    esc(collectionAddress)));

            bookingSection =
                "<div style='background:#f8faff;border:1px solid #e3eaf7;border-radius:12px;padding:18px 20px;margin-bottom:18px;'>"
                + "<div style='font-size:11px;text-transform:uppercase;letter-spacing:1px;color:#1a73e8;font-weight:700;"
                + "margin-bottom:12px;padding-bottom:8px;border-bottom:1.5px solid #e3eaf7;'>📅 Appointment Details</div>"
                + "<table style='width:100%;border-collapse:collapse;'>" + br + "</table></div>";
        }

        // ── Accent colour: blue for booking, green for prescription ──────────
        String accent = isBooking ? "#1a73e8" : "#00b894";
        String hdrGrad = isBooking
            ? "linear-gradient(135deg,#1a73e8,#0d47a1)"
            : "linear-gradient(135deg,#00b894,#00cec9)";
        String hdrLabel = isBooking ? "Booking Payment Confirmed" : "Prescription Payment Confirmed";

        return "<!DOCTYPE html><html><head><meta charset='UTF-8'/>"
            + "<style>body{font-family:'Segoe UI',Arial,sans-serif;background:#f0f4ff;margin:0;padding:0}</style>"
            + "</head><body>"
            + "<div style='max-width:620px;margin:32px auto;background:#fff;border-radius:16px;"
            + "overflow:hidden;box-shadow:0 8px 32px rgba(0,0,0,.1);'>"

            // Header
            + "<div style='background:" + hdrGrad + ";padding:32px 40px;text-align:center;'>"
            + "<div style='font-size:26px;font-weight:800;color:#fff;'>💚 LabChain</div>"
            + "<div style='color:rgba(255,255,255,.8);font-size:13px;margin-top:4px;'>" + hdrLabel + "</div>"
            + "<div style='display:inline-block;background:#d4edda;color:#155724;border-radius:20px;"
            + "padding:7px 22px;font-weight:700;font-size:14px;margin-top:16px;'>✅ Payment Successful</div>"
            + "</div>"

            // Body
            + "<div style='padding:28px 36px;'>"

            // Amount box
            + "<div style='background:linear-gradient(135deg,#e8f5e9,#f0fff4);border:2px solid #a8d5b5;"
            + "border-radius:12px;padding:18px;text-align:center;margin-bottom:20px;'>"
            + "<div style='font-size:11px;text-transform:uppercase;color:#666;letter-spacing:1px;'>Amount Received</div>"
            + "<div style='font-size:36px;font-weight:800;color:#00b894;margin-top:4px;'>₹" + amount + "</div>"
            + "</div>"

            // ── WHAT WAS BOOKED — most important block ───────────────────────
            + bookedHighlight

            // Patient card
            + "<div style='background:#f8faff;border:1px solid #e3eaf7;border-radius:12px;padding:18px 20px;margin-bottom:18px;'>"
            + "<div style='font-size:11px;text-transform:uppercase;letter-spacing:1px;color:" + accent + ";font-weight:700;"
            + "margin-bottom:12px;padding-bottom:8px;border-bottom:1.5px solid #e3eaf7;'>👤 Patient Details</div>"
            + "<table style='width:100%;border-collapse:collapse;'>"
            + tr("Name",     esc(userName))
            + tr("Phone",    esc(userPhone))
            + tr("Paid At",  timeNow)
            + "</table></div>"

            // Booking details (booking flow only)
            + bookingSection

            // Payment details card
            + "<div style='background:#f8faff;border:1px solid #e3eaf7;border-radius:12px;padding:18px 20px;margin-bottom:18px;'>"
            + "<div style='font-size:11px;text-transform:uppercase;letter-spacing:1px;color:" + accent + ";font-weight:700;"
            + "margin-bottom:12px;padding-bottom:8px;border-bottom:1.5px solid #e3eaf7;'>💳 Payment Details</div>"
            + "<table style='width:100%;border-collapse:collapse;'>"
            + tr("Razorpay Order ID", "<span style='font-family:monospace;font-size:11px;'>" + esc(orderId)   + "</span>")
            + tr("Payment ID",        "<span style='font-family:monospace;font-size:11px;'>" + esc(paymentId) + "</span>")
            + tr("Amount",            "<span style='color:#00b894;font-weight:700;font-size:15px;'>₹" + amount + "</span>")
            + tr("Status",            "<span style='color:#00b894;font-weight:700;'>✅ Verified</span>")
            + "</table></div>"

            + "</div>" // end body padding

            + "<div style='background:#f4f6fb;padding:18px 36px;text-align:center;font-size:12px;"
            + "color:#999;border-top:1px solid #e8eaf0;'>LabChain · Auto Notification · © 2025</div>"
            + "</div></body></html>";
    }

    private String tr(String label, String value) {
        return "<tr>"
            + "<td style='padding:8px 4px;font-size:13px;color:#666;width:42%;border-bottom:1px solid #eef1f8;'>" + label + "</td>"
            + "<td style='padding:8px 4px;font-size:13px;color:#222;font-weight:500;text-align:right;border-bottom:1px solid #eef1f8;'>" + value + "</td>"
            + "</tr>";
    }
}