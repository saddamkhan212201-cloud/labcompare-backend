package com.labcompare.repository;

import com.labcompare.model.Test;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface TestRepository extends JpaRepository<Test, Long> {
    List<Test> findByNameContainingIgnoreCaseOrCategoryContainingIgnoreCase(String name, String category);
    List<Test> findByCategoryIgnoreCase(String category);
    Optional<Test> findByNameIgnoreCase(String name);
}