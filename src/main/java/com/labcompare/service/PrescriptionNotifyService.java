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

    private final HttpClient   http   = HttpClient.newHttpClient();
    private final ObjectMapper mapper = new ObjectMapper();

    // ─────────────────────────────────────────────────────────────────────────
    // Legacy endpoint — kept for any direct /api/prescription/notify calls.
    // NOT called from the Razorpay payment flow any more.
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

        post(buildPayload(
            "LabChain Prescriptions <" + fromEmail + ">",
            subject, html, attachments
        ), userName, userPhone);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Called from RazorpayController after HMAC verification succeeds.
    //
    // Sends EXACTLY ONE email to the team containing:
    //   • All payment details
    //   • Patient details
    //   • Test list (prescription flow) OR booking details (booking flow)
    //   • Prescription image as attachment (prescription flow only)
    //     — imageBase64 is null for the booking flow, so no attachment there.
    //
    // Flow detection:
    //   bookingRef != null  → Compare Test booking flow
    //   bookingRef == null  → Prescription scanner flow
    // ─────────────────────────────────────────────────────────────────────────
    @Async
    public void sendPaymentSuccessToTeam(
            String       userName,
            String       userPhone,
            List<String> tests,
            long         amountInRupees,
            String       razorpayOrderId,
            String       razorpayPaymentId,
            // Prescription image — null for booking flow
            String       imageBase64,
            String       imageMime,
            String       imageName,
            // Booking context — null for prescription flow
            String       bookingRef,
            String       labName,
            String       appointmentDate,
            String       appointmentSlot,
            String       collectionType,
            String       collectionAddress) {

        try {
            boolean isBooking = bookingRef != null && !bookingRef.isBlank();
            String  timeNow   = now();

            String subject = isBooking
                ? "✅ Booking Paid ₹" + amountInRupees + " — " + userName + " | Ref: " + bookingRef
                : "✅ Prescription Payment ₹" + amountInRupees + " — " + userName + " | " + userPhone;

            String html = buildPaymentHtml(
                    userName, userPhone, tests, amountInRupees,
                    razorpayOrderId, razorpayPaymentId, timeNow,
                    bookingRef, labName, appointmentDate, appointmentSlot,
                    collectionType, collectionAddress);

            // ── Build attachments list ───────────────────────────────────────
            // Prescription flow: attach the actual prescription image.
            // Booking flow:      no image — imageBase64 is null.
            List<Map<String, String>> attachments = new ArrayList<>();

            if (imageBase64 != null && !imageBase64.isBlank()) {
                String mime = (imageMime != null && !imageMime.isBlank()) ? imageMime : "image/jpeg";
                String name = (imageName != null && !imageName.isBlank()) ? imageName : "prescription.jpg";
                attachments.add(Map.of(
                    "filename",     name,
                    "content",      imageBase64,
                    "content_type", mime
                ));
            }

            post(buildPayload(
                "LabChain Payments <" + fromEmail + ">",
                subject, html,
                attachments.isEmpty() ? null : attachments
            ), userName, userPhone);

        } catch (Exception e) {
            log.error("[PrescriptionNotify] ❌ Payment email failed for {} {}: {}",
                    userName, userPhone, e.getMessage());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Internal helpers
    // ─────────────────────────────────────────────────────────────────────────
    private Map<String, Object> buildPayload(String from, String subject,
                                              String html,
                                              List<Map<String, String>> attachments) {
        if (attachments != null && !attachments.isEmpty()) {
            return Map.of(
                "from", from, "to", List.of(teamEmail),
                "subject", subject, "html", html,
                "attachments", attachments
            );
        }
        return Map.of(
            "from", from, "to", List.of(teamEmail),
            "subject", subject, "html", html
        );
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
            log.info("[PrescriptionNotify] ✅ Email sent for {} | {}", name, phone);
        } else {
            log.error("[PrescriptionNotify] ❌ Resend {} | body={}", response.statusCode(), response.body());
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
    // HTML: payment success email — single email for both flows
    // ─────────────────────────────────────────────────────────────────────────
    private String buildPaymentHtml(String userName, String userPhone,
                                     List<String> tests, long amount,
                                     String orderId, String paymentId, String timeNow,
                                     String bookingRef, String labName,
                                     String appointmentDate, String appointmentSlot,
                                     String collectionType, String collectionAddress) {

        boolean isBooking = bookingRef != null && !bookingRef.isBlank();

        // ── Test rows ────────────────────────────────────────────────────────
        StringBuilder testRows = new StringBuilder();
        if (!isBooking) {
            if (tests != null && !tests.isEmpty()) {
                for (int i = 0; i < tests.size(); i++) {
                    String bg = (i % 2 == 0) ? "#ffffff" : "#f8faff";
                    testRows.append("<tr style='background:").append(bg).append(";'>")
                        .append("<td style='padding:9px 14px;color:#888;font-size:13px;border-bottom:1px solid #eef1f8;'>").append(i + 1).append("</td>")
                        .append("<td style='padding:9px 14px;color:#1a1a2e;font-size:13px;font-weight:500;border-bottom:1px solid #eef1f8;'>").append(esc(tests.get(i))).append("</td>")
                        .append("</tr>");
                }
            } else {
                testRows.append("<tr><td colspan='2' style='padding:14px;color:#888;font-size:13px;text-align:center;'>")
                    .append("No tests extracted — see prescription image attached</td></tr>");
            }
        }

        // ── Booking detail rows ──────────────────────────────────────────────
        StringBuilder bookingRows = new StringBuilder();
        if (isBooking) {
            bookingRows.append(tdRow("Booking Ref", "<span style='font-family:monospace;font-weight:700;'>" + esc(bookingRef) + "</span>"));
            if (labName != null)          bookingRows.append(tdRow("Lab", esc(labName)));
            if (appointmentDate != null)  bookingRows.append(tdRow("Date", esc(appointmentDate)));
            if (appointmentSlot != null)  bookingRows.append(tdRow("Time Slot", esc(appointmentSlot)));
            if (collectionType  != null)  bookingRows.append(tdRow("Collection", esc(collectionType)));
            if (collectionAddress != null && !collectionAddress.isBlank())
                                           bookingRows.append(tdRow("Address", esc(collectionAddress)));
        }

        // ── Conditional sections ─────────────────────────────────────────────
        String contextSection = isBooking
            ? "<div style='background:#f8faff;border:1px solid #e3eaf7;border-radius:12px;padding:18px 20px;margin-bottom:18px;'>"
              + "<div style='font-size:11px;text-transform:uppercase;letter-spacing:1px;color:#00b894;font-weight:700;margin-bottom:12px;padding-bottom:8px;border-bottom:1.5px solid #e3eaf7;'>📅 Booking Details</div>"
              + "<table style='width:100%;border-collapse:collapse;'>" + bookingRows + "</table></div>"
            : "<div style='background:#f8faff;border:1px solid #e3eaf7;border-radius:12px;padding:18px 20px;margin-bottom:18px;'>"
              + "<div style='font-size:11px;text-transform:uppercase;letter-spacing:1px;color:#00b894;font-weight:700;margin-bottom:8px;padding-bottom:8px;border-bottom:1.5px solid #e3eaf7;'>🧪 Tests Requested</div>"
              + "<p style='font-size:12px;color:#888;margin:0 0 12px;'>Prescription image is attached to this email.</p>"
              + "<table style='width:100%;border-collapse:collapse;border:1px solid #e3eaf7;border-radius:8px;overflow:hidden;'>"
              + "<thead><tr style='background:#00b894;'>"
              + "<th style='padding:10px 14px;color:#fff;font-size:12px;text-align:left;width:36px;'>#</th>"
              + "<th style='padding:10px 14px;color:#fff;font-size:12px;text-align:left;'>Test Name</th>"
              + "</tr></thead><tbody>" + testRows + "</tbody></table></div>";

        String intro = isBooking
            ? "Booking <strong>" + esc(bookingRef) + "</strong> is confirmed and paid. Please prepare for the appointment."
            : "Please contact the patient and process the tests listed below. Prescription image is attached.";

        return "<!DOCTYPE html><html><head><meta charset='UTF-8'/>"
            + "<style>body{font-family:'Segoe UI',Arial,sans-serif;background:#f0f4ff;margin:0;padding:0}</style>"
            + "</head><body>"
            + "<div style='max-width:620px;margin:32px auto;background:#fff;border-radius:16px;overflow:hidden;box-shadow:0 8px 32px rgba(26,115,232,.12);'>"

            // Header
            + "<div style='background:linear-gradient(135deg,#00b894,#00cec9);padding:32px 40px;text-align:center;'>"
            + "<div style='font-size:26px;font-weight:800;color:#fff;'>💚 LabChain</div>"
            + "<div style='color:rgba(255,255,255,.8);font-size:13px;margin-top:4px;'>"
            + (isBooking ? "Booking Payment Confirmed" : "Prescription Payment Confirmed") + "</div>"
            + "<div style='display:inline-block;background:#d4edda;color:#155724;border-radius:20px;padding:7px 22px;font-weight:700;font-size:14px;margin-top:16px;'>✅ Payment Successful</div>"
            + "</div>"

            // Body
            + "<div style='padding:28px 36px;'>"

            // Amount box
            + "<div style='background:linear-gradient(135deg,#e8f5e9,#f0fff4);border:2px solid #a8d5b5;border-radius:12px;padding:20px;text-align:center;margin-bottom:22px;'>"
            + "<div style='font-size:12px;text-transform:uppercase;color:#666;letter-spacing:1px;'>Amount Received</div>"
            + "<div style='font-size:38px;font-weight:800;color:#00b894;margin-top:6px;'>₹" + amount + "</div>"
            + "</div>"

            // Intro
            + "<p style='font-size:15px;color:#444;margin-bottom:22px;line-height:1.6;'>"
            + "Payment received from <strong style='color:#00b894;'>" + esc(userName) + "</strong>. " + intro + "</p>"

            // Patient card
            + "<div style='background:#f8faff;border:1px solid #e3eaf7;border-radius:12px;padding:18px 20px;margin-bottom:18px;'>"
            + "<div style='font-size:11px;text-transform:uppercase;letter-spacing:1px;color:#00b894;font-weight:700;margin-bottom:12px;padding-bottom:8px;border-bottom:1.5px solid #e3eaf7;'>👤 Patient Details</div>"
            + "<table style='width:100%;border-collapse:collapse;'>"
            + tdRow("Patient Name", esc(userName))
            + tdRow("Phone Number", esc(userPhone))
            + tdRow("Paid At",      timeNow)
            + "</table></div>"

            // Context section (booking details OR test list)
            + contextSection

            // Payment details card
            + "<div style='background:#f8faff;border:1px solid #e3eaf7;border-radius:12px;padding:18px 20px;margin-bottom:18px;'>"
            + "<div style='font-size:11px;text-transform:uppercase;letter-spacing:1px;color:#00b894;font-weight:700;margin-bottom:12px;padding-bottom:8px;border-bottom:1.5px solid #e3eaf7;'>💳 Payment Details</div>"
            + "<table style='width:100%;border-collapse:collapse;'>"
            + tdRow("Razorpay Order ID", "<span style='font-family:monospace;font-size:11px;'>" + esc(orderId)   + "</span>")
            + tdRow("Payment ID",        "<span style='font-family:monospace;font-size:11px;'>" + esc(paymentId) + "</span>")
            + tdRow("Amount",            "<span style='color:#00b894;font-weight:700;font-size:15px;'>₹" + amount + "</span>")
            + tdRow("Status",            "<span style='color:#00b894;font-weight:700;'>✅ Verified</span>")
            + "</table></div>"

            + "</div>" // end body padding

            + "<div style='background:#f4f6fb;padding:20px 36px;text-align:center;font-size:12px;color:#999;border-top:1px solid #e8eaf0;'>"
            + "LabChain · Auto Notification · © 2025 All rights reserved</div>"
            + "</div></body></html>";
    }

    /** Helper: single table row with label on left, value on right */
    private String tdRow(String label, String value) {
        return "<tr>"
            + "<td style='padding:8px 4px;font-size:13px;color:#666;width:45%;border-bottom:1px solid #eef1f8;'>" + label + "</td>"
            + "<td style='padding:8px 4px;font-size:13px;color:#222;font-weight:500;text-align:right;border-bottom:1px solid #eef1f8;'>" + value + "</td>"
            + "</tr>";
    }
}