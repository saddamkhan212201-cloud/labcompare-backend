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
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.List;
import java.util.Map;

@Service
public class PrescriptionNotifyService {

    private static final Logger log = LoggerFactory.getLogger(PrescriptionNotifyService.class);

    private static final String RESEND_API_URL = "https://api.resend.com/emails";

    @Value("${resend.api-key}")
    private String resendApiKey;

    @Value("${labcompare.app.from-email}")
    private String fromEmail;

    @Value("${app.prescription.team-email}")
    private String teamEmail;

    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Async
    public void sendPrescriptionToTeam(String userName, String userPhone, MultipartFile file) throws Exception {
        String timeNow = LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a"));

        String subject = "New Prescription - " + userName + " | " + userPhone;

        String html = buildHtml(userName, userPhone, timeNow);

        // Encode attachment as base64
        String filename = file.getOriginalFilename() != null ? file.getOriginalFilename() : "prescription.jpg";
        String base64Content = Base64.getEncoder().encodeToString(file.getBytes());
        String mimeType = file.getContentType() != null ? file.getContentType() : "image/jpeg";

        Map<String, Object> payload = Map.of(
            "from", "LabChain Prescriptions <" + fromEmail + ">",
            "to", List.of(teamEmail),
            "subject", subject,
            "html", html,
            "attachments", List.of(Map.of(
                "filename", filename,
                "content", base64Content,
                "content_type", mimeType
            ))
        );

        String body = objectMapper.writeValueAsString(payload);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(RESEND_API_URL))
                .header("Authorization", "Bearer " + resendApiKey)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 200 || response.statusCode() == 201) {
            log.info("[PrescriptionNotify] ✅ Sent via Resend for {} | {}", userName, userPhone);
        } else {
            log.error("[PrescriptionNotify] ❌ Resend error {}: {}", response.statusCode(), response.body());
            throw new Exception("Resend API error " + response.statusCode() + ": " + response.body());
        }
    }

    private String buildHtml(String userName, String userPhone, String timeNow) {
        return "<!DOCTYPE html><html><head><meta charset='UTF-8'/><style>" +
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
            "<div class='hdr'><h1>LabChain</h1><p>Prescription Notification</p>" +
            "<div class='badge'>New Prescription Received</div></div>" +
            "<div class='bdy'>" +
            "<p style='font-size:15px;color:#444;margin-bottom:20px'>A new prescription has been submitted. Please review and contact the patient.</p>" +
            "<div class='card'><div class='ct'>Patient Details</div>" +
            "<div class='row'><span>Name</span><span>" + escHtml(userName) + "</span></div>" +
            "<div class='row'><span>Phone</span><span>" + escHtml(userPhone) + "</span></div>" +
            "<div class='row'><span>Submitted At</span><span>" + timeNow + "</span></div></div>" +
            "<div class='note'>The prescription image is attached to this email.<br/>" +
            "Please call <strong>" + escHtml(userPhone) + "</strong> to assist the patient.</div>" +
            "</div>" +
            "<div class='ftr'>LabChain Prescription System - Auto Notification</div>" +
            "</div></body></html>";
    }

    private String escHtml(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}