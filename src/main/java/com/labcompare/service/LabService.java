package com.labcompare.service;

import com.labcompare.dto.*;
import com.labcompare.model.Lab;
import com.labcompare.repository.LabRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class LabService {

    private final LabRepository labRepository;

    public LabService(LabRepository labRepository) {
        this.labRepository = labRepository;
    }

    public List<LabDTO> getAllLabs() {
        return labRepository.findAll().stream().map(this::toDTO).collect(Collectors.toList());
    }

    public LabDTO getLabById(Long id) { return toDTO(findById(id)); }

    public LabDTO createLab(LabRequest req) {
        Lab lab = new Lab();
        lab.setName(req.getName());
        lab.setCity(req.getCity());
        lab.setAddress(req.getAddress());
        lab.setPhone(req.getPhone());
        lab.setRating(req.getRating() != null ? req.getRating() : 4.0);
        lab.setAccreditation(req.getAccreditation() != null ? req.getAccreditation() : "NABL");
        lab.setHomeCollection(req.getHomeCollection() != null ? req.getHomeCollection() : true);
        return toDTO(labRepository.save(lab));
    }

    public LabDTO updateLab(Long id, LabRequest req) {
        Lab lab = findById(id);
        if (req.getName() != null) lab.setName(req.getName());
        if (req.getCity() != null) lab.setCity(req.getCity());
        if (req.getAddress() != null) lab.setAddress(req.getAddress());
        if (req.getPhone() != null) lab.setPhone(req.getPhone());
        if (req.getRating() != null) lab.setRating(req.getRating());
        if (req.getAccreditation() != null) lab.setAccreditation(req.getAccreditation());
        if (req.getHomeCollection() != null) lab.setHomeCollection(req.getHomeCollection());
        return toDTO(labRepository.save(lab));
    }

    public void deleteLab(Long id) { labRepository.deleteById(id); }

    private Lab findById(Long id) {
        return labRepository.findById(id).orElseThrow(() -> new EntityNotFoundException("Lab not found: " + id));
    }

    public LabDTO toDTO(Lab lab) {
        LabDTO dto = new LabDTO();
        dto.setId(lab.getId());
        dto.setName(lab.getName());
        dto.setCity(lab.getCity());
        dto.setAddress(lab.getAddress());
        dto.setPhone(lab.getPhone());
        dto.setRating(lab.getRating());
        dto.setAccreditation(lab.getAccreditation());
        dto.setHomeCollection(lab.getHomeCollection());
        return dto;
    }
}
