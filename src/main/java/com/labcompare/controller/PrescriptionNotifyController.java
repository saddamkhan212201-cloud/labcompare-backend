package com.labcompare.controller;

import com.labcompare.service.PrescriptionNotifyService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api/prescription")
@CrossOrigin(origins = "*")
public class PrescriptionNotifyController {

    private final PrescriptionNotifyService notifyService;

    public PrescriptionNotifyController(PrescriptionNotifyService notifyService) {
        this.notifyService = notifyService;
    }

    @PostMapping("/notify")
    public ResponseEntity<?> notify(
            @RequestParam("userName") String userName,
            @RequestParam("userPhone") String userPhone,
            @RequestParam("file") MultipartFile file) {

        if (userName == null || userName.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Name is required"));
        }
        if (userPhone == null || userPhone.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Phone is required"));
        }
        if (file == null || file.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Prescription file is required"));
        }

        try {
            notifyService.sendPrescriptionToTeam(userName.trim(), userPhone.trim(), file);
            return ResponseEntity.ok(Map.of("success", true, "message", "Prescription sent to team successfully"));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("success", false, "message", "Failed to send: " + e.getMessage()));
        }
    }
}