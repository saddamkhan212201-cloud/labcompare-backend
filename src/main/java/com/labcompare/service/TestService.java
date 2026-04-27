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

    public TestDTO createTest(TestRequest req) {
        Test test = new Test();
        test.setName(req.getName());
        test.setCategory(req.getCategory());
        test.setDescription(req.getDescription());
        return toDTO(testRepository.save(test));
    }

    public TestDTO updateTest(Long id, TestRequest req) {
        Test test = findById(id);
        if (req.getName() != null) test.setName(req.getName());
        if (req.getCategory() != null) test.setCategory(req.getCategory());
        if (req.getDescription() != null) test.setDescription(req.getDescription());
        return toDTO(testRepository.save(test));
    }

    public void deleteTest(Long id) { testRepository.deleteById(id); }

    private Test findById(Long id) {
        return testRepository.findById(id).orElseThrow(() -> new EntityNotFoundException("Test not found: " + id));
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
