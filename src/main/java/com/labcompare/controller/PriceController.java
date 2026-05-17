package com.labcompare.controller;

import com.labcompare.dto.*;
import com.labcompare.service.PriceService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/prices")
@CrossOrigin(origins = "*")
public class PriceController {

    private final PriceService priceService;

    public PriceController(PriceService priceService) {
        this.priceService = priceService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<PriceDTO>>> search(
            @RequestParam(required = false) Long testId,
            @RequestParam(required = false) String city) {
        return ResponseEntity.ok(ApiResponse.ok(priceService.searchPrices(testId, city)));
    }

    @GetMapping("/test/{testId}")
    public ResponseEntity<ApiResponse<List<PriceDTO>>> byTest(@PathVariable Long testId) {
        return ResponseEntity.ok(ApiResponse.ok(priceService.getPricesByTest(testId)));
    }

    @GetMapping("/all")
    public ResponseEntity<ApiResponse<List<PriceDTO>>> getAll() {
        return ResponseEntity.ok(ApiResponse.ok(priceService.getAllPrices()));
    }

    // Create (or upsert same lab+test)
    @PostMapping
    public ResponseEntity<ApiResponse<PriceDTO>> setPrice(@Valid @RequestBody PriceRequest req) {
        return ResponseEntity.ok(ApiResponse.ok("Price saved", priceService.setPrice(req)));
    }

    // FIX 1: Update existing price by ID
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<PriceDTO>> updatePrice(
            @PathVariable Long id, @RequestBody PriceRequest req) {
        return ResponseEntity.ok(ApiResponse.ok("Price updated", priceService.updatePrice(id, req)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        priceService.deletePrice(id);
        return ResponseEntity.ok(ApiResponse.ok("Price deleted", null));
    }
}