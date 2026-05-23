package com.labcompare.controller;

import com.labcompare.config.JwtUtil;
import com.labcompare.dto.ApiResponse;
import com.labcompare.dto.LoginRequest;
import com.labcompare.dto.LoginResponse;
import com.labcompare.model.User;
import com.labcompare.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.Random;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    private final UserRepository  userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil         jwtUtil;

    @Value("${resend.api-key}")
    private String resendApiKey;

    @Value("${labcompare.app.from-email:no-reply@labchain.in}")
    private String fromEmail;

    @Value("${labcompare.app.name:LabChain}")
    private String appName;

    private final HttpClient httpClient = HttpClient.newHttpClient();

    public AuthController(UserRepository userRepository,
                          PasswordEncoder passwordEncoder,
                          JwtUtil jwtUtil) {
        this.userRepository  = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil         = jwtUtil;
    }

    // ─── LOGIN ───────────────────────────────────────────────────────────────
    //
    // Accepts email + password (all new USER accounts).
    // Falls back to username lookup for legacy seeded admin/superadmin accounts
    // that were created before the email-login migration and have no email set.
    // Both lookup paths share the same generic error to prevent enumeration.

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(@RequestBody LoginRequest req) {

        // Accept whichever identifier the client sent
        String identifier = (req.getEmail() != null && !req.getEmail().isBlank())
                ? req.getEmail().trim().toLowerCase()
                : (req.getUsername() != null ? req.getUsername().trim() : "");

        if (identifier.isEmpty() || req.getPassword() == null || req.getPassword().isBlank())
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Email and password are required"));

        // 1. Try email lookup first (all normal user accounts)
        Optional<User> found = userRepository.findByEmail(identifier);

        // 2. Fall back to username (legacy admin/superadmin without email)
        if (found.isEmpty()) {
            found = userRepository.findByUsername(identifier);
        }

        // Same message for both "not found" and "wrong password" — no enumeration
        if (found.isEmpty() || !passwordEncoder.matches(req.getPassword(), found.get().getPassword()))
            throw new RuntimeException("Invalid email or password");

        User user = found.get();

        String token = jwtUtil.generateToken(
                user.getUsername(), user.getRole().name(), user.getAdminLabId(), user.getPhone());

        LoginResponse resp = new LoginResponse(
                token, user.getUsername(), user.getRole().name(), user.getAdminLabId(), user.getPhone());

        log.info("[Auth] Login success: identifier={} role={}", identifier, user.getRole());
        return ResponseEntity.ok(ApiResponse.ok("Login successful", resp));
    }

    // ─── REGISTER ────────────────────────────────────────────────────────────
    //
    // Email is REQUIRED — it becomes the login identifier and the recovery address.
    // Username is auto-derived from the email local-part so the rest of the system
    // (JWT subject, booking ownership, admin panel) keeps working unchanged.

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<String>> register(@RequestBody LoginRequest req) {

        // ── Validate email ────────────────────────────────────────────────
        String email = (req.getEmail() != null) ? req.getEmail().trim().toLowerCase() : "";
        if (email.isEmpty() || !email.matches("^[^@]+@[^@]+\\.[^@]+$"))
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("A valid email address is required"));

        if (userRepository.findByEmail(email).isPresent())
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("An account with this email already exists"));

        // ── Validate password ─────────────────────────────────────────────
        if (req.getPassword() == null || req.getPassword().length() < 6)
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Password must be at least 6 characters"));

        // ── Validate phone ────────────────────────────────────────────────
        if (req.getPhone() == null || !req.getPhone().matches("^[6-9]\\d{9}$"))
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Enter a valid 10-digit mobile number"));

        if (userRepository.findByPhone(req.getPhone()).isPresent())
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("This phone number is already registered"));

        // ── Derive a unique username from the email local-part ────────────
        // Strips non-alphanumeric characters then appends a numeric suffix
        // until a free slot is found. This keeps username unique without
        // exposing it to the user (they never see or type it).
        String base     = email.split("@")[0].replaceAll("[^a-zA-Z0-9_]", "").toLowerCase();
        if (base.isEmpty()) base = "user";
        String username = base;
        int    suffix   = 2;
        while (userRepository.existsByUsername(username)) {
            username = base + suffix++;
        }

        User user = new User(username, passwordEncoder.encode(req.getPassword()), User.Role.USER);
        user.setEmail(email);
        user.setPhone(req.getPhone().trim());

        userRepository.save(user);
        log.info("[Auth] Registered: username={} email={} phone={}", username, email, req.getPhone());
        return ResponseEntity.ok(ApiResponse.ok("Registration successful", "USER"));
    }

    // ─── FORGOT PASSWORD — STEP 1: send OTP ──────────────────────────────────
    //
    // Always returns HTTP 200 with a generic message regardless of whether
    // the email is registered — prevents email enumeration attacks.

    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponse<String>> forgotPassword(@RequestBody Map<String, String> req) {
        String email = req.get("email");
        if (email == null || email.isBlank())
            return ResponseEntity.badRequest().body(ApiResponse.error("Email is required"));

        User user = userRepository.findByEmail(email.trim().toLowerCase()).orElse(null);

        if (user == null) {
            log.warn("[Auth] Forgot-password: no account for email={}", email);
            return ResponseEntity.ok(ApiResponse.ok(
                    "If this email is registered you will receive an OTP shortly.", null));
        }

        // 6-digit OTP, expires in 10 minutes
        String otp = String.format("%06d", new Random().nextInt(999999));
        user.setResetOtp(otp);
        user.setResetOtpExpiresAt(LocalDateTime.now().plusMinutes(10));
        userRepository.save(user);

        try {
            sendOtpEmail(user.getEmail(), user.getUsername(), otp);
            log.info("[Auth] OTP sent to email={}", email);
        } catch (Exception e) {
            log.error("[Auth] OTP email failed for email={}: {}", email, e.getMessage());
            return ResponseEntity.status(500)
                    .body(ApiResponse.error("Failed to send OTP email. Please try again."));
        }

        return ResponseEntity.ok(ApiResponse.ok("OTP sent to your email. It expires in 10 minutes.", null));
    }

    // ─── FORGOT PASSWORD — STEP 2: verify OTP + set new password ─────────────

    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse<String>> resetPassword(@RequestBody Map<String, String> req) {
        String email       = req.get("email");
        String otp         = req.get("otp");
        String newPassword = req.get("newPassword");

        if (email == null || email.isBlank() || otp == null || newPassword == null)
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Email, OTP and new password are required"));

        if (newPassword.length() < 6)
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Password must be at least 6 characters"));

        User user = userRepository.findByEmail(email.trim().toLowerCase()).orElse(null);

        if (user == null || !user.isOtpValid(otp.trim()))
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Invalid or expired OTP. Please request a new one."));

        // Update password and clear OTP fields atomically
        user.setPassword(passwordEncoder.encode(newPassword));
        user.setResetOtp(null);
        user.setResetOtpExpiresAt(null);
        userRepository.save(user);

        log.info("[Auth] Password reset for email={}", email);
        return ResponseEntity.ok(ApiResponse.ok("Password reset successful. You can now log in.", null));
    }

    // ─── OTP EMAIL via Resend ─────────────────────────────────────────────────

    private void sendOtpEmail(String toEmail, String username, String otp) throws Exception {
        String html = """
            <!DOCTYPE html><html><head><meta charset="UTF-8"/></head>
            <body style="font-family:'Segoe UI',Arial,sans-serif;background:#f0f4ff;margin:0;padding:0;">
              <div style="max-width:460px;margin:32px auto;background:#fff;border-radius:14px;
                          overflow:hidden;box-shadow:0 4px 24px rgba(0,0,0,.09);">
                <div style="background:linear-gradient(135deg,#1D9E75,#0d6e53);padding:26px;text-align:center;">
                  <div style="font-size:21px;font-weight:800;color:#fff;">🔬 LabChain</div>
                  <div style="color:rgba(255,255,255,.8);font-size:13px;margin-top:3px;">Password Reset</div>
                </div>
                <div style="padding:28px 30px;">
                  <p style="font-size:15px;color:#444;margin:0 0 6px;">Hi <strong>%s</strong>,</p>
                  <p style="font-size:13px;color:#666;margin:0 0 20px;">Use this OTP to reset your LabChain password:</p>
                  <div style="background:#f0fff8;border:2px dashed #1D9E75;border-radius:12px;
                              padding:20px;text-align:center;margin-bottom:20px;">
                    <div style="font-size:38px;font-weight:900;letter-spacing:10px;color:#085041;
                                font-family:'Courier New',monospace;">%s</div>
                    <div style="font-size:12px;color:#888;margin-top:8px;">Expires in 10 minutes</div>
                  </div>
                  <p style="font-size:12px;color:#aaa;margin:0;">
                    If you did not request this, ignore this email. Your password will not change.
                  </p>
                </div>
                <div style="background:#f4f6fb;padding:14px;text-align:center;font-size:12px;color:#999;">
                  &copy; 2025 LabChain &middot; Auto Notification
                </div>
              </div>
            </body></html>
            """.formatted(username, otp);

        com.fasterxml.jackson.databind.ObjectMapper om = new com.fasterxml.jackson.databind.ObjectMapper();
        String body = om.writeValueAsString(Map.of(
            "from",    appName + " <" + fromEmail + ">",
            "to",      new String[]{ toEmail },
            "subject", "🔑 Your LabChain OTP: " + otp,
            "html",    html
        ));

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.resend.com/emails"))
                .header("Authorization", "Bearer " + resendApiKey)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200 && response.statusCode() != 201)
            throw new Exception("Resend error " + response.statusCode() + ": " + response.body());
    }
}