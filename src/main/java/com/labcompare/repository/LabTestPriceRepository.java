package com.labcompare.repository;

import com.labcompare.model.LabTestPrice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface LabTestPriceRepository extends JpaRepository<LabTestPrice, Long> {

    List<LabTestPrice> findByTestId(Long testId);
    List<LabTestPrice> findByLabId(Long labId);
    Optional<LabTestPrice> findByLabIdAndTestId(Long labId, Long testId);

    @Query("SELECT ltp FROM LabTestPrice ltp JOIN ltp.lab l JOIN ltp.test t " +
           "WHERE (:testId IS NULL OR t.id = :testId) " +
           "AND (:city IS NULL OR LOWER(l.city) LIKE LOWER(CONCAT('%', :city, '%'))) " +
           "ORDER BY ltp.price ASC")
    List<LabTestPrice> searchPrices(@Param("testId") Long testId, @Param("city") String city);
}
