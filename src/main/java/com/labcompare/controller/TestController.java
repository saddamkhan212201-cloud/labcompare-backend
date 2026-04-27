package com.labcompare.controller;

import com.labcompare.dto.*;
import com.labcompare.service.TestService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/tests")
@CrossOrigin(origins = "*")
public class TestController {

    private final TestService testService;

    public TestController(TestService testService) {
        this.testService = testService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<TestDTO>>> getAll(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String category) {
        List<TestDTO> result;
        if (search != null && !search.isBlank()) result = testService.searchTests(search);
        else if (category != null && !category.isBlank()) result = testService.getByCategory(category);
        else result = testService.getAllTests();
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<TestDTO>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(testService.getTestById(id)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<TestDTO>> create(@Valid @RequestBody TestRequest req) {
        return ResponseEntity.ok(ApiResponse.ok("Test created", testService.createTest(req)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<TestDTO>> update(@PathVariable Long id, @Valid @RequestBody TestRequest req) {
        return ResponseEntity.ok(ApiResponse.ok("Test updated", testService.updateTest(id, req)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        testService.deleteTest(id);
        return ResponseEntity.ok(ApiResponse.ok("Test deleted", null));
    }
}
