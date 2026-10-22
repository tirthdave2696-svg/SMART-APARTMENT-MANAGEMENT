package com.example.demo.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "user_sessions")
public class UserSession {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, unique = true, length = 100) private String token;
    @Column(nullable = false) private Long userId;
    private LocalDateTime createdAt = LocalDateTime.now();
    private LocalDateTime lastAccessAt = LocalDateTime.now();
    private LocalDateTime endedAt;
    private boolean active = true;
    private String endedReason;

    public Long getId() { return id; }
    public String getToken() { return token; } public void setToken(String v) { token = v; }
    public Long getUserId() { return userId; } public void setUserId(Long v) { userId = v; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getLastAccessAt() { return lastAccessAt; } public void setLastAccessAt(LocalDateTime v) { lastAccessAt = v; }
    public LocalDateTime getEndedAt() { return endedAt; } public void setEndedAt(LocalDateTime v) { endedAt = v; }
    public boolean isActive() { return active; } public void setActive(boolean v) { active = v; }
    public String getEndedReason() { return endedReason; } public void setEndedReason(String v) { endedReason = v; }
}