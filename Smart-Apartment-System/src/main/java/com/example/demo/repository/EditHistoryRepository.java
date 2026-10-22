package com.example.demo.repository;

import com.example.demo.model.EditHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface EditHistoryRepository extends JpaRepository<EditHistory, Long> {
    List<EditHistory> findByResidentIdOrderByChangedAtDesc(Long residentId);
}