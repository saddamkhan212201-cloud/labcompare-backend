package com.labcompare.controller;

import com.labcompare.dto.ApiResponse;
import com.labcompare.model.User;
import com.labcompare.repository.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/users")
@CrossOrigin(origins = "*")
public class UserController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserController(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /** List all users (sanitized — no passwords) */
    @GetMapping
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> listUsers() {
        List<Map<String, Object>> users = userRepository.findAll().stream()
                .map(u -> Map.<String, Object>of(
                        "id", u.getId(),
                        "username", u.getUsername(),
                        "role", u.getRole().name(),
                        "adminLabId", u.getAdminLabId() != null ? u.getAdminLabId() : ""
                ))
                .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.ok("Users fetched", users));
    }

    /**
     * Create a new user.
     * Body: { username, password, role, adminLabId? }
     */
    @PostMapping
    public ResponseEntity<ApiResponse<String>> createUser(@RequestBody Map<String, Object> body) {
        String username = (String) body.get("username");
        String password = (String) body.get("password");
        String roleStr  = (String) body.get("role");

        if (userRepository.existsByUsername(username)) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Username already exists"));
        }

        User.Role role;
        try {
            role = User.Role.valueOf(roleStr.toUpperCase());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Invalid role. Use SUPERADMIN, ADMIN, or USER"));
        }

        Long adminLabId = null;
        if (body.get("adminLabId") != null && !body.get("adminLabId").toString().isEmpty()) {
            try { adminLabId = Long.parseLong(body.get("adminLabId").toString()); }
            catch (NumberFormatException ignored) {}
        }

        User user = new User(username, passwordEncoder.encode(password), role, adminLabId);
        userRepository.save(user);
        return ResponseEntity.ok(ApiResponse.ok("User created successfully", role.name()));
    }

    /** Delete a user */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<String>> deleteUser(@PathVariable Long id) {
        if (!userRepository.existsById(id)) {
            return ResponseEntity.badRequest().body(ApiResponse.error("User not found"));
        }
        userRepository.deleteById(id);
        return ResponseEntity.ok(ApiResponse.ok("User deleted", null));
    }

    /** Update user role / adminLabId */
    @PatchMapping("/{id}")
    public ResponseEntity<ApiResponse<String>> updateUser(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        User user = userRepository.findById(id).orElseThrow(() -> new RuntimeException("User not found"));

        if (body.containsKey("role")) {
            try { user.setRole(User.Role.valueOf(body.get("role").toString().toUpperCase())); }
            catch (Exception e) { return ResponseEntity.badRequest().body(ApiResponse.error("Invalid role")); }
        }

        if (body.containsKey("adminLabId")) {
            String val = body.get("adminLabId").toString();
            user.setAdminLabId(val.isEmpty() ? null : Long.parseLong(val));
        }

        if (body.containsKey("password") && !body.get("password").toString().isEmpty()) {
            user.setPassword(passwordEncoder.encode(body.get("password").toString()));
        }

        userRepository.save(user);
        return ResponseEntity.ok(ApiResponse.ok("User updated", null));
    }
}
