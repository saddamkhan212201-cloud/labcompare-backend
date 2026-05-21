package com.labcompare.service;

import com.labcompare.dto.BookingDTO;
import com.resend.Resend;
import com.resend.core.exception.ResendException;
import com.resend.services.emails.model.CreateEmailOptions;
import com.resend.services.emails.model.CreateEmailResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;

@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final QRCodeService qrCodeService;
    private final Resend resend;

//    @Value("${labcompare.app.from-email:onboarding@resend.dev}")
//    private String fromEmail;

    
    @Value("${labcompare.app.from-email:no-reply@labchain.in}")
    private String fromEmail;
    
    @Value("${labcompare.app.support-email:support@LabChain.in}")
    private String supportEmail;

    @Value("${labcompare.app.name:LabChain}")
    private String appName;

    public EmailService(@Value("${resend.api-key}") String apiKey,
                        QRCodeService qrCodeService) {
        this.resend = new Resend(apiKey);
        this.qrCodeService = qrCodeService;
    }

    @Async
    public void sendBookingConfirmation(BookingDTO booking) {
        if (booking.getEmail() == null || booking.getEmail().isBlank()) return;
        try {
            String qrBase64 = null;
            try { qrBase64 = qrCodeService.generateBookingQR(booking); }
            catch (Exception e) { log.warn("[Email] QR failed: {}", e.getMessage()); }

            String subject = "✅ Booking Confirmed – " + booking.getTestName()
                    + " at " + booking.getLabName() + " | " + booking.getBookingRef();

            CreateEmailOptions params = CreateEmailOptions.builder()
                    .from(appName + " <" + fromEmail + ">")
                    .to(booking.getEmail())
                    .subject(subject)
                    .html(buildConfirmationHtml(booking, qrBase64))
                    .build();

            CreateEmailResponse response = resend.emails().send(params);
            log.info("[Email] ✅ Sent to {} for {} | id={}", booking.getEmail(), booking.getBookingRef(), response.getId());

        } catch (ResendException e) {
            log.error("[Email] ❌ Failed for {}: {}", booking.getBookingRef(), e.getMessage());
        }
    }

    @Async
    public void sendCancellationEmail(BookingDTO booking) {
        if (booking.getEmail() == null || booking.getEmail().isBlank()) return;
        try {
            String subject = "❌ Booking Cancelled – " + booking.getTestName()
                    + " | " + booking.getBookingRef();

            CreateEmailOptions params = CreateEmailOptions.builder()
                    .from(appName + " <" + fromEmail + ">")
                    .to(booking.getEmail())
                    .subject(subject)
                    .html(buildCancellationHtml(booking))
                    .build();

            CreateEmailResponse response = resend.emails().send(params);
            log.info("[Email] ✅ Cancellation sent to {} | id={}", booking.getEmail(), response.getId());

        } catch (ResendException e) {
            log.error("[Email] ❌ Cancellation failed: {}", e.getMessage());
        }
    }

    private String buildConfirmationHtml(BookingDTO b, String qrBase64) {
        String dateStr = b.getAppointmentDate() != null
                ? b.getAppointmentDate().format(DateTimeFormatter.ofPattern("EEEE, dd MMMM yyyy"))
                : "To be confirmed";
        String collectionLabel  = "HOME".equalsIgnoreCase(b.getCollectionType()) ? "🏠 Home Collection" : "🏥 Visit Lab";
        String collectionDetail = "HOME".equalsIgnoreCase(b.getCollectionType())
                ? (b.getCollectionAddress() != null ? b.getCollectionAddress() : "Address on file") : b.getLabName();
        String paymentIcon = switch (b.getPaymentMethod() != null ? b.getPaymentMethod().toUpperCase() : "") {
            case "UPI"  -> "📱 UPI"; case "CARD" -> "💳 Card"; default -> "💵 Cash";
        };
        double testPrice   = b.getTestPrice()   != null ? b.getTestPrice()   : 0.0;
        double totalAmount = b.getTotalAmount() != null ? b.getTotalAmount() : testPrice;
        String homeFeeRow  = "HOME".equalsIgnoreCase(b.getCollectionType())
                ? "<tr><td class='lb'>Home Collection Fee</td><td class='vl'>&#8377;50</td></tr>" : "";
        String bookedAt = b.getCreatedAt() != null
                ? b.getCreatedAt().format(DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a")) : "";

        String qrSection = (qrBase64 != null) ? """
            <div class="qr-box">
              <div class="qr-title">&#128241; Scan &amp; Pay with any UPI App</div>
              <div class="qr-sub">GPay &middot; PhonePe &middot; Paytm &middot; BHIM &middot; Any UPI App</div>
              <img src="data:image/png;base64,%s"
                   alt="LabCompare UPI QR"
                   style="width:210px;height:210px;display:block;margin:14px auto;
                          border:3px solid #c5d5f8;border-radius:10px;padding:6px;background:#fff;"/>
              <div class="qr-ref">%s</div>
              <div class="qr-hint">Amount: &#8377;%.0f &middot; UPI: saddamkhan212201@oksbi</div>
            </div>""".formatted(qrBase64, b.getBookingRef(), totalAmount) : "";

        return """
        <!DOCTYPE html><html lang="en"><head><meta charset="UTF-8"/>
        <meta name="viewport" content="width=device-width,initial-scale=1.0"/>
        <style>
          *{box-sizing:border-box;margin:0;padding:0;}
          body{font-family:'Segoe UI',Arial,sans-serif;background:#f0f4ff;color:#333;}
          .wrap{max-width:620px;margin:30px auto;background:#fff;border-radius:16px;
                overflow:hidden;box-shadow:0 8px 32px rgba(26,115,232,0.12);}
          .hdr{background:linear-gradient(135deg,#1a73e8,#0d47a1);padding:32px 40px;text-align:center;}
          .logo{font-size:26px;font-weight:800;color:#fff;}.logo span{color:#90caf9;}
          .tag{color:#b3d0f5;font-size:13px;margin-top:4px;}
          .badge{display:inline-block;background:#e8f5e9;color:#2e7d32;border-radius:20px;
                 padding:7px 22px;font-weight:700;font-size:14px;margin-top:16px;}
          .body{padding:28px 36px;}
          .ref-box{background:linear-gradient(135deg,#e8f0fe,#f0f4ff);border:1.5px solid #c5d5f8;
                   border-radius:12px;padding:16px;text-align:center;margin-bottom:20px;}
          .rl{font-size:11px;text-transform:uppercase;letter-spacing:1px;color:#666;margin-bottom:5px;}
          .rv{font-size:24px;font-weight:800;color:#1a73e8;letter-spacing:3px;font-family:'Courier New',monospace;}
          .rt{font-size:11px;color:#888;margin-top:5px;}
          .thl{background:linear-gradient(135deg,#e3f2fd,#e8f0fe);border-left:4px solid #1a73e8;
               border-radius:0 10px 10px 0;padding:14px 18px;margin-bottom:18px;}
          .tn{font-size:17px;font-weight:700;color:#0d47a1;}
          .ln{font-size:13px;color:#555;margin-top:3px;}
          .qr-box{background:linear-gradient(135deg,#f8faff,#e8f0fe);border:2px solid #c5d5f8;
                  border-radius:14px;padding:20px;text-align:center;margin-bottom:18px;}
          .qr-title{font-size:15px;font-weight:700;color:#1a73e8;}
          .qr-sub{font-size:11px;color:#888;margin-top:3px;}
          .qr-ref{font-size:15px;font-weight:800;color:#0d47a1;letter-spacing:2px;
                  font-family:'Courier New',monospace;margin-top:6px;}
          .qr-hint{font-size:11px;color:#666;margin-top:4px;}
          .card{background:#f8faff;border:1px solid #e3eaf7;border-radius:12px;
                padding:18px 20px;margin-bottom:18px;}
          .ct{font-size:11px;text-transform:uppercase;letter-spacing:1px;color:#1a73e8;
              font-weight:700;margin-bottom:12px;padding-bottom:8px;border-bottom:1.5px solid #e3eaf7;}
          table.d{width:100%%;border-collapse:collapse;}
          table.d td{padding:8px 4px;font-size:13px;border-bottom:1px solid #eef1f8;}
          table.d tr:last-child td{border-bottom:none;}
          td.lb{color:#666;width:50%%;}td.vl{color:#222;font-weight:500;text-align:right;}
          td.vl.hi{color:#1a73e8;font-size:17px;font-weight:800;}
          .col{background:#f3fdf4;border:1px solid #c8e6c9;border-radius:10px;padding:14px 18px;margin-bottom:18px;}
          .ct2{font-size:14px;font-weight:700;color:#2e7d32;}
          .cd{font-size:13px;color:#555;margin-top:4px;}
          .step{display:flex;align-items:flex-start;margin-bottom:8px;font-size:13px;color:#444;}
          .num{background:#1a73e8;color:#fff;border-radius:50%%;width:20px;height:20px;min-width:20px;
               font-size:11px;font-weight:700;text-align:center;line-height:20px;margin-right:8px;margin-top:1px;}
          .tip{background:#fff8e1;border-left:4px solid #f9a825;border-radius:0 8px 8px 0;
               padding:12px 16px;font-size:13px;color:#555;margin-bottom:16px;line-height:1.5;}
          .ftr{background:#f4f6fb;padding:20px 36px;text-align:center;font-size:12px;
               color:#999;border-top:1px solid #e8eaf0;}
          .ftr a{color:#1a73e8;text-decoration:none;}
        </style></head><body>
        <div class="wrap">
          <div class="hdr">
            <div class="logo">&#128302; Lab<span>Compare</span></div>
            <div class="tag">Lab Test Price Comparison &amp; Booking</div>
            <div class="badge">&#9989; Booking Confirmed</div>
          </div>
          <div class="body">
            <p style="font-size:15px;color:#444;line-height:1.6;margin-bottom:20px;">
              Hi <strong style="color:#1a73e8;">%s</strong>,<br/>
              Your booking is <strong>confirmed</strong>! Scan the QR below to pay instantly.</p>
            <div class="ref-box">
              <div class="rl">Booking Reference</div>
              <div class="rv">%s</div>
              %s
            </div>
            <div class="thl">
              <div class="tn">&#129514; %s</div>
              <div class="ln">&#128205; %s</div>
            </div>
            %s
            <div class="card">
              <div class="ct">&#128197; Appointment Details</div>
              <table class="d">
                <tr><td class="lb">Date</td><td class="vl">%s</td></tr>
                <tr><td class="lb">Time Slot</td><td class="vl">&#128336; %s</td></tr>
                <tr><td class="lb">Lab</td><td class="vl">%s</td></tr>
                <tr><td class="lb">Patient</td><td class="vl">%s</td></tr>
                <tr><td class="lb">Age</td><td class="vl">%s Yrs</td></tr>
                <tr><td class="lb">Phone</td><td class="vl">%s</td></tr>
              </table>
            </div>
            <div class="col">
              <div class="ct2">%s</div>
              <div class="cd">%s</div>
            </div>
            <div class="card">
              <div class="ct">&#128179; Payment Summary</div>
              <table class="d">
                <tr><td class="lb">%s</td><td class="vl">&#8377;%.0f</td></tr>
                %s
                <tr><td class="lb" style="font-weight:700;color:#333;">Total</td>
                    <td class="vl hi">&#8377;%.0f</td></tr>
                <tr><td class="lb">Payment</td><td class="vl">%s</td></tr>
              </table>
            </div>
            <div style="margin-bottom:16px;">
              <div style="font-size:12px;font-weight:700;color:#333;margin-bottom:10px;
                          text-transform:uppercase;letter-spacing:0.5px;">&#128203; What to do next</div>
              <div class="step"><div class="num">1</div>Scan QR above to pay OR show Booking ID <strong>%s</strong></div>
              <div class="step"><div class="num">2</div>Fast 8-12 hours if your test requires it</div>
              <div class="step"><div class="num">3</div>Carry a valid photo ID to the lab</div>
              <div class="step"><div class="num">4</div>Arrive 10 minutes before your slot</div>
            </div>
            <div class="tip">
              &#128161; Scan the QR with GPay, PhonePe or Paytm. Amount &#8377;%.0f is pre-filled - one tap to pay!
            </div>
          </div>
          <div class="ftr">
            Automated confirmation from <strong>LabCompare</strong><br/>
            Questions? <a href="mailto:%s">%s</a><br/>
            <span style="color:#bbb;">&#169; 2025 LabCompare &middot; All rights reserved</span>
          </div>
        </div></body></html>
        """.formatted(
                b.getPatientName() != null ? b.getPatientName() : "Valued Customer",
                b.getBookingRef(),
                bookedAt.isBlank() ? "" : "<div class='rt'>Booked on " + bookedAt + "</div>",
                b.getTestName(), b.getLabName(),
                qrSection,
                dateStr,
                b.getAppointmentSlot() != null ? b.getAppointmentSlot() : "-",
                b.getLabName(),
                b.getPatientName() != null ? b.getPatientName() : "-",
                b.getPatientAge() != null ? b.getPatientAge() : "-",
                b.getPhone() != null ? b.getPhone() : "-",
                collectionLabel, collectionDetail,
                b.getTestName(), testPrice, homeFeeRow, totalAmount, paymentIcon,
                b.getBookingRef(), totalAmount,
                supportEmail, supportEmail
        );
    }

    private String buildCancellationHtml(BookingDTO b) {
        double total = b.getTotalAmount() != null ? b.getTotalAmount() : 0.0;
        return """
        <!DOCTYPE html><html><head><meta charset="UTF-8"/>
        <style>
          body{font-family:'Segoe UI',Arial,sans-serif;background:#f4f4f4;}
          .w{max-width:600px;margin:30px auto;background:#fff;border-radius:12px;
             overflow:hidden;box-shadow:0 4px 20px rgba(0,0,0,0.08);}
          .h{background:linear-gradient(135deg,#e53935,#b71c1c);padding:28px 36px;text-align:center;}
          .h h1{color:#fff;margin:0;font-size:20px;}
          .badge{display:inline-block;background:#fce4ec;color:#c62828;border-radius:18px;
                 padding:5px 18px;font-weight:700;font-size:14px;margin-top:14px;}
          .b{padding:28px 36px;}
          .rb{background:#fff3e0;border:1.5px solid #ffe0b2;border-radius:10px;
              padding:14px;text-align:center;margin-bottom:20px;}
          .rl{font-size:11px;color:#888;text-transform:uppercase;letter-spacing:1px;}
          .rv{font-size:20px;font-weight:800;color:#e65100;letter-spacing:2px;font-family:'Courier New',monospace;}
          .card{background:#fafafa;border:1px solid #eee;border-radius:10px;padding:18px 20px;margin-bottom:16px;}
          .row{display:flex;justify-content:space-between;padding:6px 0;border-bottom:1px solid #f0f0f0;font-size:14px;}
          .row:last-child{border-bottom:none;}
          .note{background:#e8f5e9;border-left:4px solid #43a047;border-radius:0 8px 8px 0;
                padding:12px 14px;font-size:13px;color:#2e7d32;margin-bottom:16px;}
          .f{background:#f4f6fb;padding:18px 36px;text-align:center;font-size:12px;color:#999;}
          .f a{color:#1a73e8;text-decoration:none;}
        </style></head><body>
        <div class="w">
          <div class="h"><h1>&#128302; LabCompare</h1><div class="badge">&#10060; Booking Cancelled</div></div>
          <div class="b">
            <p style="font-size:14px;color:#444;margin-bottom:18px;">
              Hi <strong>%s</strong>,<br/>Your booking has been <strong>cancelled</strong>.</p>
            <div class="rb"><div class="rl">Cancelled Booking</div><div class="rv">%s</div></div>
            <div class="card">
              <div class="row"><span>Test</span><span><strong>%s</strong></span></div>
              <div class="row"><span>Lab</span><span>%s</span></div>
              <div class="row"><span>Amount</span><span>&#8377;%.0f</span></div>
              <div class="row"><span>Status</span><span style="color:#e53935;font-weight:700;">Cancelled</span></div>
            </div>
            <div class="note">&#128154; <strong>Refund:</strong> If paid online, refund within 5-7 business days.</div>
          </div>
          <div class="f">Questions? <a href="mailto:%s">%s</a><br/>&#169; 2025 LabCompare</div>
        </div></body></html>
        """.formatted(
                b.getPatientName() != null ? b.getPatientName() : "Valued Customer",
                b.getBookingRef(), b.getTestName(), b.getLabName(), total,
                supportEmail, supportEmail
        );
    }
}