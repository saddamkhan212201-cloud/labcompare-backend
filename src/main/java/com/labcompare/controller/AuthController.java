package com.labcompare.controller;

import com.labcompare.config.JwtUtil;
import com.labcompare.dto.ApiResponse;
import com.labcompare.dto.LoginRequest;
import com.labcompare.dto.LoginResponse;
import com.labcompare.model.User;
import com.labcompare.repository.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
public class AuthController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public AuthController(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtUtil jwtUtil) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(@RequestBody LoginRequest req) {
        User user = userRepository.findByUsername(req.getUsername())
                .orElseThrow(() -> new RuntimeException("Invalid username or password"));

        if (!passwordEncoder.matches(req.getPassword(), user.getPassword())) {
            throw new RuntimeException("Invalid username or password");
        }

        String token = jwtUtil.generateToken(user.getUsername(), user.getRole().name(), user.getAdminLabId());
        LoginResponse resp = new LoginResponse(token, user.getUsername(), user.getRole().name(), user.getAdminLabId());
        return ResponseEntity.ok(ApiResponse.ok("Login successful", resp));
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<String>> register(@RequestBody LoginRequest req) {
        if (userRepository.existsByUsername(req.getUsername())) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Username already exists"));
        }
        User user = new User(req.getUsername(), passwordEncoder.encode(req.getPassword()), User.Role.USER);
        userRepository.save(user);
        return ResponseEntity.ok(ApiResponse.ok("Registration successful", "USER"));
    }
}
