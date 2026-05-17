package com.labcompare.service;

import com.labcompare.dto.*;
import com.labcompare.model.Test;
import com.labcompare.repository.TestRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class TestService {

    private final TestRepository testRepository;

    public TestService(TestRepository testRepository) {
        this.testRepository = testRepository;
    }

    public List<TestDTO> getAllTests() {
        return testRepository.findAll().stream().map(this::toDTO).collect(Collectors.toList());
    }

    public List<TestDTO> searchTests(String query) {
        return testRepository.findByNameContainingIgnoreCaseOrCategoryContainingIgnoreCase(query, query)
                .stream().map(this::toDTO).collect(Collectors.toList());
    }

    public List<TestDTO> getByCategory(String category) {
        return testRepository.findByCategoryIgnoreCase(category).stream().map(this::toDTO).collect(Collectors.toList());
    }

    public TestDTO getTestById(Long id) { return toDTO(findById(id)); }

    // FIX 4: Duplicate prevention on create — reject if same name already exists (case-insensitive)
    public TestDTO createTest(TestRequest req) {
        testRepository.findByNameIgnoreCase(req.getName().trim()).ifPresent(existing -> {
            throw new IllegalArgumentException(
                "A test named \"" + existing.getName() + "\" already exists (Category: " + existing.getCategory() + ")");
        });
        Test test = new Test();
        test.setName(req.getName().trim());
        test.setCategory(req.getCategory().trim());
        test.setDescription(req.getDescription() != null ? req.getDescription().trim() : "");
        return toDTO(testRepository.save(test));
    }

    // FIX 4: Duplicate prevention on update — reject if name clashes with a DIFFERENT test
    public TestDTO updateTest(Long id, TestRequest req) {
        Test test = findById(id);
        if (req.getName() != null && !req.getName().isBlank()) {
            String newName = req.getName().trim();
            testRepository.findByNameIgnoreCase(newName).ifPresent(existing -> {
                if (!existing.getId().equals(id)) {
                    throw new IllegalArgumentException(
                        "A test named \"" + existing.getName() + "\" already exists (Category: " + existing.getCategory() + ")");
                }
            });
            test.setName(newName);
        }
        if (req.getCategory() != null && !req.getCategory().isBlank())
            test.setCategory(req.getCategory().trim());
        if (req.getDescription() != null)
            test.setDescription(req.getDescription().trim());
        return toDTO(testRepository.save(test));
    }

    public void deleteTest(Long id) { testRepository.deleteById(id); }

    private Test findById(Long id) {
        return testRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Test not found: " + id));
    }

    public TestDTO toDTO(Test test) {
        TestDTO dto = new TestDTO();
        dto.setId(test.getId());
        dto.setName(test.getName());
        dto.setCategory(test.getCategory());
        dto.setDescription(test.getDescription());
        return dto;
    }
}