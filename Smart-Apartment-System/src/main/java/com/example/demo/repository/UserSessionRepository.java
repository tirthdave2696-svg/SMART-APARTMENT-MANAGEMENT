package com.example.demo.repository;

import com.example.demo.model.UserSession;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;

public interface UserSessionRepository extends JpaRepository<UserSession, Long> {
    Optional<UserSession> findByTokenAndActiveTrue(String token);
    List<UserSession> findByUserIdAndActiveTrue(Long userId);
    List<UserSession> findByUserIdOrderByCreatedAtDesc(Long userId);
}