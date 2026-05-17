package com.labcompare.service;

import com.labcompare.dto.*;
import com.labcompare.model.Lab;
import com.labcompare.repository.BookingRepository;
import com.labcompare.repository.LabRepository;
import com.labcompare.repository.LabTestPriceRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class LabService {

    private final LabRepository          labRepository;
    private final BookingRepository      bookingRepository;
    private final LabTestPriceRepository priceRepository;

    public LabService(LabRepository labRepository,
                      BookingRepository bookingRepository,
                      LabTestPriceRepository priceRepository) {
        this.labRepository    = labRepository;
        this.bookingRepository = bookingRepository;
        this.priceRepository  = priceRepository;
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
        if (req.getName()          != null) lab.setName(req.getName());
        if (req.getCity()          != null) lab.setCity(req.getCity());
        if (req.getAddress()       != null) lab.setAddress(req.getAddress());
        if (req.getPhone()         != null) lab.setPhone(req.getPhone());
        if (req.getRating()        != null) lab.setRating(req.getRating());
        if (req.getAccreditation() != null) lab.setAccreditation(req.getAccreditation());
        if (req.getHomeCollection()!= null) lab.setHomeCollection(req.getHomeCollection());
        return toDTO(labRepository.save(lab));
    }

    /**
     * Force-delete a lab along with ALL its associated data:
     *  1. Bookings referencing this lab  (fixes FK constraint on bookings.lab_id)
     *  2. Prices for this lab            (handled by CascadeType.ALL on Lab.prices,
     *                                     but we delete explicitly to be safe)
     *  3. The lab itself
     *
     * All three steps run inside one transaction — if anything fails,
     * nothing is deleted.
     */
    public void deleteLab(Long id) {
        // Verify lab exists first
        findById(id);

        // Step 1: delete all bookings linked to this lab
        List<com.labcompare.model.Booking> bookings = bookingRepository.findByLabId(id);
        if (!bookings.isEmpty()) {
            bookingRepository.deleteAll(bookings);
        }

        // Step 2: delete all prices linked to this lab
        List<com.labcompare.model.LabTestPrice> prices = priceRepository.findByLabId(id);
        if (!prices.isEmpty()) {
            priceRepository.deleteAll(prices);
        }

        // Step 3: now safe to delete the lab
        labRepository.deleteById(id);
    }

    private Lab findById(Long id) {
        return labRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Lab not found: " + id));
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