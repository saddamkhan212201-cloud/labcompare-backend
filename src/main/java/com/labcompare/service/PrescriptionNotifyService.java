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
import java.util.Base64;
import java.util.List;
import java.util.Map;

@Service
public class PrescriptionNotifyService {

    private static final Logger log      = LoggerFactory.getLogger(PrescriptionNotifyService.class);
    private static final String RESEND   = "https://api.resend.com/emails";

    @Value("${resend.api-key}")
    private String resendApiKey;

    @Value("${labcompare.app.from-email}")
    private String fromEmail;

    @Value("${app.prescription.team-email}")
    private String teamEmail;

    private final HttpClient   httpClient   = HttpClient.newHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();

    // ─────────────────────────────────────────────────────────────────────────
    // Called from PrescriptionNotifyController (POST /api/prescription/notify)
    // Sends the prescription image to the team as an email attachment.
    // This is now also called by the frontend after Razorpay payment succeeds,
    // so the team gets the actual prescription image — not just a test list.
    // ─────────────────────────────────────────────────────────────────────────
    @Async
    public void sendPrescriptionToTeam(String userName, String userPhone,
                                        MultipartFile file) throws Exception {
        String timeNow   = now();
        String subject   = "New Prescription — " + userName + " | " + userPhone;
        String html      = buildPrescriptionHtml(userName, userPhone, timeNow);
        String filename  = file.getOriginalFilename() != null ? file.getOriginalFilename() : "prescription.jpg";
        String b64       = Base64.getEncoder().encodeToString(file.getBytes());
        String mime      = file.getContentType() != null ? file.getContentType() : "image/jpeg";

        Map<String, Object> payload = Map.of(
            "from",        "LabChain Prescriptions <" + fromEmail + ">",
            "to",          List.of(teamEmail),
            "subject",     subject,
            "html",        html,
            "attachments", List.of(Map.of("filename", filename, "content", b64, "content_type", mime))
        );

        post(payload, userName, userPhone);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Called from RazorpayController after HMAC payment verification succeeds.
    //
    // Sends team email with:
    //  • Rich HTML — patient + payment details + test table
    //  • Plain-text .txt attachment — full test list / booking summary
    //
    // Works for BOTH flows:
    //   a) Prescription Scanner  → bookingRef/labName/etc. are null
    //   b) Compare Test Booking  → bookingRef/labName/etc. are populated
    //
    // After this, the prescription frontend also calls /api/prescription/notify
    // separately to attach the actual image — two emails total for scanner flow,
    // one for booking flow (image not relevant there).
    // ─────────────────────────────────────────────────────────────────────────
    @Async
    public void sendPaymentSuccessToTeam(String userName,
                                          String userPhone,
                                          List<String> tests,
                                          long          amountInRupees,
                                          String        razorpayOrderId,
                                          String        razorpayPaymentId,
                                          // Optional — null for prescription scanner flow
                                          String        bookingRef,
                                          String        labName,
                                          String        appointmentDate,
                                          String        appointmentSlot,
                                          String        collectionType,
                                          String        collectionAddress) {
        try {
            String timeNow = now();
            boolean isBookingFlow = bookingRef != null && !bookingRef.isBlank();

            String subject = isBookingFlow
                ? "✅ Booking Payment ₹" + amountInRupees + " — " + userName + " | Ref: " + bookingRef
                : "✅ Payment ₹" + amountInRupees + " Received — " + userName + " | " + userPhone;

            String html = buildPaymentSuccessHtml(
                    userName, userPhone, tests, amountInRupees,
                    razorpayOrderId, razorpayPaymentId, timeNow,
                    bookingRef, labName, appointmentDate, appointmentSlot,
                    collectionType, collectionAddress);

            // ── Build plain-text attachment (test list or booking summary) ──
            StringBuilder txt = new StringBuilder();
            txt.append("LabChain — ").append(isBookingFlow ? "Booking Summary" : "Test List").append("\n");
            txt.append("═══════════════════════════════════════════════\n");
            txt.append("Patient  : ").append(userName).append("\n");
            txt.append("Phone    : ").append(userPhone).append("\n");
            txt.append("Amount   : ₹").append(amountInRupees).append("\n");
            txt.append("Order ID : ").append(razorpayOrderId).append("\n");
            txt.append("Pay ID   : ").append(razorpayPaymentId).append("\n");
            txt.append("Time     : ").append(timeNow).append("\n");
            if (isBookingFlow) {
                txt.append("Booking  : ").append(bookingRef).append("\n");
                if (labName != null)         txt.append("Lab      : ").append(labName).append("\n");
                if (appointmentDate != null) txt.append("Date     : ").append(appointmentDate).append("\n");
                if (appointmentSlot != null) txt.append("Slot     : ").append(appointmentSlot).append("\n");
                if (collectionType  != null) txt.append("Type     : ").append(collectionType).append("\n");
                if (collectionAddress != null && !collectionAddress.isBlank())
                    txt.append("Address  : ").append(collectionAddress).append("\n");
            }
            txt.append("═══════════════════════════════════════════════\n\n");
            txt.append("Tests Requested:\n");
            if (tests != null && !tests.isEmpty()) {
                for (int i = 0; i < tests.size(); i++) {
                    txt.append(String.format("  %2d. %s%n", i + 1, tests.get(i)));
                }
            } else {
                txt.append("  (No tests extracted — see prescription attachment)\n");
            }
            txt.append("\n═══════════════════════════════════════════════\n");

            String attachName = isBookingFlow
                ? "booking-" + bookingRef + ".txt"
                : "test-list-" + userName.replaceAll("\\s+", "_") + ".txt";
            String attachB64 = Base64.getEncoder()
                    .encodeToString(txt.toString().getBytes(StandardCharsets.UTF_8));

            Map<String, Object> payload = Map.of(
                "from",        "LabChain Payments <" + fromEmail + ">",
                "to",          List.of(teamEmail),
                "subject",     subject,
                "html",        html,
                "attachments", List.of(Map.of(
                    "filename",     attachName,
                    "content",      attachB64,
                    "content_type", "text/plain"
                ))
            );

            post(payload, userName, userPhone);

        } catch (Exception e) {
            log.error("[PrescriptionNotify] ❌ Payment email failed for {} {}: {}",
                    userName, userPhone, e.getMessage());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // INTERNAL helpers
    // ─────────────────────────────────────────────────────────────────────────
    private void post(Map<String, Object> payload, String name, String phone) throws Exception {
        String body = objectMapper.writeValueAsString(payload);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(RESEND))
                .header("Authorization", "Bearer " + resendApiKey)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 200 || response.statusCode() == 201) {
            log.info("[PrescriptionNotify] ✅ Email sent for {} | {}", name, phone);
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
    // HTML — prescription-only email (no payment, image attached separately)
    // ─────────────────────────────────────────────────────────────────────────
    private String buildPrescriptionHtml(String userName, String userPhone, String timeNow) {
        return "<!DOCTYPE html><html><head><meta charset='UTF-8'/><style>" +
            "body{font-family:'Segoe UI',Arial,sans-serif;background:#f4f6fb;margin:0;padding:0}" +
            ".wrap{max-width:600px;margin:32px auto;background:#fff;border-radius:14px;overflow:hidden;box-shadow:0 4px 24px rgba(0,0,0,.09)}" +
            ".hdr{background:linear-gradient(135deg,#6c63ff,#a78bfa);padding:32px 40px;text-align:center}" +
            ".hdr h1{color:#fff;margin:0;font-size:22px}" +
            ".hdr p{color:rgba(255,255,255,.8);margin:6px 0 0;font-size:14px}" +
            ".badge{display:inline-block;padding:7px 22px;border-radius:20px;font-weight:700;font-size:14px;margin-top:16px;background:#e8f5e9;color:#2e7d32}" +
            ".bdy{padding:32px 40px}" +
            ".card{background:#f8faff;border:1px solid #e3eaf7;border-radius:10px;padding:20px 24px;margin-bottom:18px}" +
            ".ct{font-size:11px;text-transform:uppercase;color:#888;letter-spacing:.8px;margin-bottom:14px;font-weight:700}" +
            ".row{display:flex;justify-content:space-between;padding:7px 0;border-bottom:1px solid #eef1f8;font-size:14px}" +
            ".row:last-child{border-bottom:none}" +
            ".row span:first-child{color:#666}.row span:last-child{color:#222;font-weight:600}" +
            ".note{background:#fff8e1;border-left:4px solid #f9a825;border-radius:0 8px 8px 0;padding:12px 16px;font-size:13px;color:#555;margin-bottom:20px;line-height:1.6}" +
            ".ftr{background:#f4f6fb;padding:18px 40px;text-align:center;font-size:12px;color:#999}" +
            "</style></head><body><div class='wrap'>" +
            "<div class='hdr'><h1>LabChain</h1><p>Prescription Notification</p><div class='badge'>📋 New Prescription Received</div></div>" +
            "<div class='bdy'>" +
            "<p style='font-size:15px;color:#444;margin-bottom:20px'>A new prescription has been submitted. Please review and contact the patient.</p>" +
            "<div class='card'><div class='ct'>Patient Details</div>" +
            "<div class='row'><span>Name</span><span>" + esc(userName) + "</span></div>" +
            "<div class='row'><span>Phone</span><span>" + esc(userPhone) + "</span></div>" +
            "<div class='row'><span>Submitted At</span><span>" + timeNow + "</span></div></div>" +
            "<div class='note'>📎 The prescription image is attached to this email.<br/>Please call <strong>" + esc(userPhone) + "</strong> to assist the patient.</div>" +
            "</div><div class='ftr'>LabChain Prescription System · Auto Notification</div></div></body></html>";
    }

    // ─────────────────────────────────────────────────────────────────────────
    // HTML — payment success email with full context.
    // Shows booking details card when called from booking flow.
    // ─────────────────────────────────────────────────────────────────────────
    private String buildPaymentSuccessHtml(String userName, String userPhone,
                                            List<String> tests, long amount,
                                            String orderId, String paymentId,
                                            String timeNow,
                                            String bookingRef, String labName,
                                            String appointmentDate, String appointmentSlot,
                                            String collectionType, String collectionAddress) {

        boolean isBookingFlow = bookingRef != null && !bookingRef.isBlank();

        // Test rows table
        StringBuilder rows = new StringBuilder();
        if (tests != null && !tests.isEmpty()) {
            for (int i = 0; i < tests.size(); i++) {
                String bg = (i % 2 == 0) ? "#ffffff" : "#f8faff";
                rows.append("<tr style='background:").append(bg).append(";'>")
                    .append("<td style='padding:9px 14px;color:#888;font-size:13px;border-bottom:1px solid #eef1f8;'>").append(i + 1).append("</td>")
                    .append("<td style='padding:9px 14px;color:#1a1a2e;font-size:13px;font-weight:500;border-bottom:1px solid #eef1f8;'>").append(esc(tests.get(i))).append("</td>")
                    .append("</tr>");
            }
        } else {
            rows.append("<tr><td colspan='2' style='padding:14px;color:#888;font-size:13px;text-align:center;'>No tests extracted — see prescription attachment</td></tr>");
        }

        // Booking details card (only for booking flow)
        String bookingSection = "";
        if (isBookingFlow) {
            StringBuilder br = new StringBuilder();
            br.append("<tr><td style='padding:8px 4px;font-size:13px;color:#666;width:45%;border-bottom:1px solid #eef1f8;'>Booking Ref</td>")
              .append("<td style='padding:8px 4px;font-size:13px;color:#222;font-weight:700;text-align:right;border-bottom:1px solid #eef1f8;font-family:monospace;'>").append(esc(bookingRef)).append("</td></tr>");
            if (labName != null)
                br.append("<tr><td style='padding:8px 4px;font-size:13px;color:#666;border-bottom:1px solid #eef1f8;'>Lab</td>")
                  .append("<td style='padding:8px 4px;font-size:13px;color:#222;font-weight:500;text-align:right;border-bottom:1px solid #eef1f8;'>").append(esc(labName)).append("</td></tr>");
            if (appointmentDate != null)
                br.append("<tr><td style='padding:8px 4px;font-size:13px;color:#666;border-bottom:1px solid #eef1f8;'>Date</td>")
                  .append("<td style='padding:8px 4px;font-size:13px;color:#222;font-weight:500;text-align:right;border-bottom:1px solid #eef1f8;'>").append(esc(appointmentDate)).append("</td></tr>");
            if (appointmentSlot != null)
                br.append("<tr><td style='padding:8px 4px;font-size:13px;color:#666;border-bottom:1px solid #eef1f8;'>Slot</td>")
                  .append("<td style='padding:8px 4px;font-size:13px;color:#222;font-weight:500;text-align:right;border-bottom:1px solid #eef1f8;'>").append(esc(appointmentSlot)).append("</td></tr>");
            if (collectionType != null)
                br.append("<tr><td style='padding:8px 4px;font-size:13px;color:#666;border-bottom:1px solid #eef1f8;'>Collection</td>")
                  .append("<td style='padding:8px 4px;font-size:13px;color:#222;font-weight:500;text-align:right;border-bottom:1px solid #eef1f8;'>").append(esc(collectionType)).append("</td></tr>");
            if (collectionAddress != null && !collectionAddress.isBlank())
                br.append("<tr><td style='padding:8px 4px;font-size:13px;color:#666;'>Address</td>")
                  .append("<td style='padding:8px 4px;font-size:13px;color:#222;font-weight:500;text-align:right;'>").append(esc(collectionAddress)).append("</td></tr>");

            bookingSection = "<div style='background:#f8faff;border:1px solid #e3eaf7;border-radius:12px;padding:18px 20px;margin-bottom:18px;'>"
                + "<div style='font-size:11px;text-transform:uppercase;letter-spacing:1px;color:#00b894;font-weight:700;margin-bottom:12px;padding-bottom:8px;border-bottom:1.5px solid #e3eaf7;'>📅 Booking Details</div>"
                + "<table style='width:100%;border-collapse:collapse;'>" + br + "</table></div>";
        }

        return "<!DOCTYPE html><html><head><meta charset='UTF-8'/>"
            + "<style>body{font-family:'Segoe UI',Arial,sans-serif;background:#f0f4ff;margin:0;padding:0}</style>"
            + "</head><body>"
            + "<div style='max-width:620px;margin:32px auto;background:#fff;border-radius:16px;overflow:hidden;box-shadow:0 8px 32px rgba(26,115,232,.12);'>"

            // header
            + "<div style='background:linear-gradient(135deg,#00b894,#00cec9);padding:32px 40px;text-align:center;'>"
            + "<div style='font-size:26px;font-weight:800;color:#fff;'>💚 LabChain</div>"
            + "<div style='color:rgba(255,255,255,.8);font-size:13px;margin-top:4px;'>Payment Confirmation</div>"
            + "<div style='display:inline-block;background:#d4edda;color:#155724;border-radius:20px;padding:7px 22px;font-weight:700;font-size:14px;margin-top:16px;'>✅ Payment Successful</div>"
            + "</div>"

            // body
            + "<div style='padding:28px 36px;'>"

            // amount box
            + "<div style='background:linear-gradient(135deg,#e8f5e9,#f0fff4);border:2px solid #a8d5b5;border-radius:12px;padding:20px;text-align:center;margin-bottom:22px;'>"
            + "<div style='font-size:12px;text-transform:uppercase;color:#666;letter-spacing:1px;'>Amount Received</div>"
            + "<div style='font-size:38px;font-weight:800;color:#00b894;margin-top:6px;'>₹" + amount + "</div>"
            + "</div>"

            // intro
            + "<p style='font-size:15px;color:#444;margin-bottom:22px;line-height:1.6;'>"
            + "Payment received from <strong style='color:#00b894;'>" + esc(userName) + "</strong>. "
            + (isBookingFlow
                ? "Booking <strong>" + esc(bookingRef) + "</strong> is confirmed. Please prepare for the appointment."
                : "Please contact the patient and process the tests listed below.")
            + "</p>"

            // patient details card
            + "<div style='background:#f8faff;border:1px solid #e3eaf7;border-radius:12px;padding:18px 20px;margin-bottom:18px;'>"
            + "<div style='font-size:11px;text-transform:uppercase;letter-spacing:1px;color:#00b894;font-weight:700;margin-bottom:12px;padding-bottom:8px;border-bottom:1.5px solid #e3eaf7;'>👤 Patient Details</div>"
            + "<table style='width:100%;border-collapse:collapse;'>"
            + "<tr><td style='padding:8px 4px;font-size:13px;color:#666;width:45%;border-bottom:1px solid #eef1f8;'>Patient Name</td>"
            +     "<td style='padding:8px 4px;font-size:13px;color:#222;font-weight:500;text-align:right;border-bottom:1px solid #eef1f8;'>" + esc(userName) + "</td></tr>"
            + "<tr><td style='padding:8px 4px;font-size:13px;color:#666;border-bottom:1px solid #eef1f8;'>Phone Number</td>"
            +     "<td style='padding:8px 4px;font-size:13px;color:#222;font-weight:500;text-align:right;border-bottom:1px solid #eef1f8;'>" + esc(userPhone) + "</td></tr>"
            + "<tr><td style='padding:8px 4px;font-size:13px;color:#666;'>Paid At</td>"
            +     "<td style='padding:8px 4px;font-size:13px;color:#222;font-weight:500;text-align:right;'>" + timeNow + "</td></tr>"
            + "</table></div>"

            // booking details (only for booking flow)
            + bookingSection

            // payment details card
            + "<div style='background:#f8faff;border:1px solid #e3eaf7;border-radius:12px;padding:18px 20px;margin-bottom:18px;'>"
            + "<div style='font-size:11px;text-transform:uppercase;letter-spacing:1px;color:#00b894;font-weight:700;margin-bottom:12px;padding-bottom:8px;border-bottom:1.5px solid #e3eaf7;'>💳 Payment Details</div>"
            + "<table style='width:100%;border-collapse:collapse;'>"
            + "<tr><td style='padding:8px 4px;font-size:13px;color:#666;width:45%;border-bottom:1px solid #eef1f8;'>Razorpay Order ID</td>"
            +     "<td style='padding:8px 4px;font-size:11px;color:#555;font-family:monospace;text-align:right;border-bottom:1px solid #eef1f8;'>" + esc(orderId) + "</td></tr>"
            + "<tr><td style='padding:8px 4px;font-size:13px;color:#666;border-bottom:1px solid #eef1f8;'>Payment ID</td>"
            +     "<td style='padding:8px 4px;font-size:11px;color:#555;font-family:monospace;text-align:right;border-bottom:1px solid #eef1f8;'>" + esc(paymentId) + "</td></tr>"
            + "<tr><td style='padding:8px 4px;font-size:13px;color:#666;border-bottom:1px solid #eef1f8;'>Amount</td>"
            +     "<td style='padding:8px 4px;font-size:16px;color:#00b894;font-weight:700;text-align:right;border-bottom:1px solid #eef1f8;'>₹" + amount + "</td></tr>"
            + "<tr><td style='padding:8px 4px;font-size:13px;color:#666;'>Status</td>"
            +     "<td style='padding:8px 4px;font-size:13px;color:#00b894;font-weight:700;text-align:right;'>✅ Verified</td></tr>"
            + "</table></div>"

            // test list card
            + "<div style='background:#f8faff;border:1px solid #e3eaf7;border-radius:12px;padding:18px 20px;margin-bottom:18px;'>"
            + "<div style='font-size:11px;text-transform:uppercase;letter-spacing:1px;color:#00b894;font-weight:700;margin-bottom:8px;padding-bottom:8px;border-bottom:1.5px solid #e3eaf7;'>🧪 Tests Requested</div>"
            + "<p style='font-size:12px;color:#888;margin:0 0 12px;'>Also attached as a file for easy reference.</p>"
            + "<table style='width:100%;border-collapse:collapse;border-radius:8px;overflow:hidden;border:1px solid #e3eaf7;'>"
            + "<thead><tr style='background:#00b894;'>"
            + "<th style='padding:10px 14px;color:#fff;font-size:12px;text-align:left;font-weight:600;width:36px;'>#</th>"
            + "<th style='padding:10px 14px;color:#fff;font-size:12px;text-align:left;font-weight:600;'>Test Name</th>"
            + "</tr></thead>"
            + "<tbody>" + rows + "</tbody>"
            + "</table></div>"

            + "</div>" // end body padding

            // footer
            + "<div style='background:#f4f6fb;padding:20px 36px;text-align:center;font-size:12px;color:#999;border-top:1px solid #e8eaf0;'>"
            + "LabChain Payment System · Auto Notification<br/>© 2025 LabChain · All rights reserved</div>"

            + "</div></body></html>";
    }
}