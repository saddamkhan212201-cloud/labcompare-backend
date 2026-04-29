//package com.labcompare.repository;
//
//import com.labcompare.model.LabTestPrice;
//import org.springframework.data.jpa.repository.JpaRepository;
//import org.springframework.data.jpa.repository.Query;
//import org.springframework.data.repository.query.Param;
//import org.springframework.stereotype.Repository;
//
//import java.util.List;
//import java.util.Optional;
//
//@Repository
//public interface LabTestPriceRepository extends JpaRepository<LabTestPrice, Long> {
//
//    List<LabTestPrice> findByTestId(Long testId);
//
//    List<LabTestPrice> findByLabId(Long labId);
//
//    Optional<LabTestPrice> findByLabIdAndTestId(Long labId, Long testId);
//
//    @Query(value = "SELECT * FROM lab_test_prices ltp " +
//           "JOIN labs l ON l.id = ltp.lab_id " +
//           "JOIN tests t ON t.id = ltp.test_id " +
//           "WHERE (CAST(:testId AS BIGINT) IS NULL OR t.id = :testId) " +
//           "AND (:city IS NULL OR LOWER(l.city) LIKE LOWER(CONCAT('%', :city, '%'))) " +
//           "ORDER BY ltp.price ASC", nativeQuery = true)
//    List<LabTestPrice> searchPrices(@Param("testId") Long testId, @Param("city") String city);
//}




package com.labcompare.repository;

import com.labcompare.model.LabTestPrice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface LabTestPriceRepository extends JpaRepository<LabTestPrice, Long> {

    List<LabTestPrice> findByTestId(Long testId);
    List<LabTestPrice> findByLabId(Long labId);
    Optional<LabTestPrice> findByLabIdAndTestId(Long labId, Long testId);

    List<LabTestPrice> findByTestIdOrderByPriceAsc(Long testId);
    List<LabTestPrice> findByTestIdAndLab_CityContainingIgnoreCaseOrderByPriceAsc(Long testId, String city);
    List<LabTestPrice> findByLab_CityContainingIgnoreCaseOrderByPriceAsc(String city);
}