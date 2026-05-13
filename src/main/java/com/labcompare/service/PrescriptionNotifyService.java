package com.labcompare.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
public class PrescriptionNotifyService {

    private static final Logger log = LoggerFactory.getLogger(PrescriptionNotifyService.class);

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username:your-email@gmail.com}")
    private String fromEmail;

    // Your team email - prescriptions will be sent here
    @Value("${app.prescription.team-email:saddamkhan212201@gmail.com}")
    private String teamEmail;

    public PrescriptionNotifyService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    @Async
    public void sendPrescriptionToTeam(String userName, String userPhone, MultipartFile file) throws Exception {
        try {
            String timeNow = LocalDateTime.now()
                    .format(DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a"));

            String subject = "📋 New Prescription – " + userName + " | " + userPhone;

            String html = "<!DOCTYPE html><html><head><meta charset='UTF-8'/><style>" +
                "body{font-family:'Segoe UI',Arial,sans-serif;background:#f4f6fb;margin:0;padding:0}" +
                ".wrap{max-width:600px;margin:32px auto;background:#fff;border-radius:14px;overflow:hidden;box-shadow:0 4px 24px rgba(0,0,0,0.09)}" +
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
                "<div class='hdr'><h1>🔬 LabChain</h1><p>Prescription Notification</p>" +
                "<div class='badge'>📋 New Prescription Received</div></div>" +
                "<div class='bdy'>" +
                "<p style='font-size:15px;color:#444;margin-bottom:20px'>A new prescription has been submitted. Please review and contact the patient.</p>" +
                "<div class='card'><div class='ct'>Patient Details</div>" +
                "<div class='row'><span>👤 Name</span><span>" + escHtml(userName) + "</span></div>" +
                "<div class='row'><span>📞 Phone</span><span>" + escHtml(userPhone) + "</span></div>" +
                "<div class='row'><span>🕐 Submitted At</span><span>" + timeNow + "</span></div></div>" +
                "<div class='note'>📎 The prescription image is attached to this email.<br/>" +
                "Please call <strong>" + escHtml(userPhone) + "</strong> to assist the patient.</div>" +
                "</div>" +
                "<div class='ftr'>LabChain Prescription System – Auto Notification</div>" +
                "</div></body></html>";

            MimeMessage msg = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(msg, true, "UTF-8");
            helper.setFrom(fromEmail, "LabChain Prescriptions");
            helper.setTo(teamEmail);
            helper.setSubject(subject);
            helper.setText(html, true);

            // Attach the prescription image/pdf
            helper.addAttachment(
                file.getOriginalFilename() != null ? file.getOriginalFilename() : "prescription.jpg",
                file
            );

            mailSender.send(msg);
            log.info("[PrescriptionNotify] ✅ Sent to team for {} | {}", userName, userPhone);

        } catch (MessagingException | java.io.UnsupportedEncodingException e) {
            log.error("[PrescriptionNotify] ❌ Failed for {}: {}", userPhone, e.getMessage());
            throw new Exception("Failed to send email: " + e.getMessage());
        }
    }

    private String escHtml(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}