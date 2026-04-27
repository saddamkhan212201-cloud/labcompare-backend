package com.labcompare.controller;

import com.labcompare.dto.*;
import com.labcompare.service.LabService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/labs")
@CrossOrigin(origins = "*")
public class LabController {

    private final LabService labService;

    public LabController(LabService labService) {
        this.labService = labService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<LabDTO>>> getAll() {
        return ResponseEntity.ok(ApiResponse.ok(labService.getAllLabs()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<LabDTO>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(labService.getLabById(id)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<LabDTO>> create(@Valid @RequestBody LabRequest req) {
        return ResponseEntity.ok(ApiResponse.ok("Lab created", labService.createLab(req)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<LabDTO>> update(@PathVariable Long id, @Valid @RequestBody LabRequest req) {
        return ResponseEntity.ok(ApiResponse.ok("Lab updated", labService.updateLab(id, req)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        labService.deleteLab(id);
        return ResponseEntity.ok(ApiResponse.ok("Lab deleted", null));
    }
}
