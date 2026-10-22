package com.example.demo.repository;

import com.example.demo.model.Complaint;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ComplaintRepository extends JpaRepository<Complaint, Long> {
    List<Complaint> findAllByOrderByCreatedAtDesc();
    List<Complaint> findByResidentIdOrderByCreatedAtDesc(Long residentId);
    List<Complaint> findByAssignedToIdOrderByCreatedAtDesc(Long staffId);
    List<Complaint> findByResidentId(Long residentId);
    boolean existsByAssignedToIdAndUnitNumber(Long staffId, String unitNumber);
}