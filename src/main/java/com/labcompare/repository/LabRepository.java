package com.labcompare.repository;

import com.labcompare.model.Lab;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface LabRepository extends JpaRepository<Lab, Long> {
    List<Lab> findByCityIgnoreCase(String city);
    List<Lab> findByCityIgnoreCaseContaining(String city);
}
