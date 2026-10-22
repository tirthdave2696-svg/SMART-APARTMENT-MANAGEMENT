package com.example.demo.repository;

import com.example.demo.model.VisitRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface VisitRequestRepository extends JpaRepository<VisitRequest, Long> {
    List<VisitRequest> findAllByOrderByCreatedAtDesc();
    List<VisitRequest> findByResidentIdOrderByCreatedAtDesc(Long residentId);
    List<VisitRequest> findByResidentId(Long residentId);
}