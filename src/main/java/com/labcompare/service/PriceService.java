package com.labcompare.service;

import com.labcompare.dto.*;
import com.labcompare.model.*;
import com.labcompare.repository.*;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class PriceService {

    private final LabTestPriceRepository priceRepository;
    private final LabRepository labRepository;
    private final TestRepository testRepository;

    public PriceService(LabTestPriceRepository priceRepository, LabRepository labRepository, TestRepository testRepository) {
        this.priceRepository = priceRepository;
        this.labRepository = labRepository;
        this.testRepository = testRepository;
    }

    public List<PriceDTO> getAllPrices() {
        return priceRepository.findAll().stream().map(this::toDTO).collect(Collectors.toList());
    }

    public List<PriceDTO> searchPrices(Long testId, String city) {
        return priceRepository.searchPrices(testId, city).stream().map(this::toDTO).collect(Collectors.toList());
    }

    public List<PriceDTO> getPricesByTest(Long testId) {
        return priceRepository.findByTestId(testId).stream().map(this::toDTO).collect(Collectors.toList());
    }

    public PriceDTO setPrice(PriceRequest req) {
        Lab lab = labRepository.findById(req.getLabId()).orElseThrow(() -> new EntityNotFoundException("Lab not found"));
        Test test = testRepository.findById(req.getTestId()).orElseThrow(() -> new EntityNotFoundException("Test not found"));
        LabTestPrice price = priceRepository.findByLabIdAndTestId(req.getLabId(), req.getTestId())
                .orElse(new LabTestPrice());
        price.setLab(lab);
        price.setTest(test);
        price.setPrice(req.getPrice());
        price.setDiscountPercent(req.getDiscountPercent() != null ? req.getDiscountPercent() : 0.0);
        price.setReportDuration(req.getReportDuration() != null ? req.getReportDuration() : "Same Day");
        return toDTO(priceRepository.save(price));
    }

    public void deletePrice(Long id) { priceRepository.deleteById(id); }

    public PriceDTO toDTO(LabTestPrice p) {
        PriceDTO dto = new PriceDTO();
        dto.setId(p.getId());
        dto.setLabId(p.getLab().getId());
        dto.setLabName(p.getLab().getName());
        dto.setLabCity(p.getLab().getCity());
        dto.setLabAccreditation(p.getLab().getAccreditation());
        dto.setLabRating(p.getLab().getRating());
        dto.setTestId(p.getTest().getId());
        dto.setTestName(p.getTest().getName());
        dto.setTestCategory(p.getTest().getCategory());
        dto.setPrice(p.getPrice());
        dto.setDiscountPercent(p.getDiscountPercent());
        dto.setEffectivePrice(p.getEffectivePrice());
        dto.setReportDuration(p.getReportDuration());
        return dto;
    }
}
