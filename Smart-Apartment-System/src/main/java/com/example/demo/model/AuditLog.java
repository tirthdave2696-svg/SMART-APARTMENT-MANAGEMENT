package com.example.demo.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "audit_logs")
public class AuditLog {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long actorId;
    @Column(nullable = false, length = 40) private String action;
    private Long targetUserId;
    @Column(length = 500) private String details;
    private LocalDateTime createdAt = LocalDateTime.now();

    public AuditLog() {}
    public AuditLog(Long actorId, String action, Long targetUserId, String details) {
        this.actorId = actorId; this.action = action; this.targetUserId = targetUserId; this.details = details;
    }
    public Long getId() { return id; }
    public Long getActorId() { return actorId; }
    public String getAction() { return action; }
    public Long getTargetUserId() { return targetUserId; }
    public String getDetails() { return details; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}