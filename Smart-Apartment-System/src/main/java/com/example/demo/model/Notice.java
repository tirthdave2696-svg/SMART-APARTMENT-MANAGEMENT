package com.example.demo.model;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "notices")
public class Notice {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, length = 200) private String title;
    @Column(nullable = false, length = 2000) private String message;
    @Column(nullable = false, length = 20) private String noticeType = "GENERAL";
    @Column(nullable = false, length = 30) private String audience = "ALL";
    private LocalDate expiresOn;
    private Long createdById;
    private String createdByName;
    private LocalDateTime createdAt = LocalDateTime.now();
    private LocalDateTime updatedAt = LocalDateTime.now();

    public Long getId() { return id; }
    public String getTitle() { return title; } public void setTitle(String v) { title = v; }
    public String getMessage() { return message; } public void setMessage(String v) { message = v; }
    public String getNoticeType() { return noticeType; } public void setNoticeType(String v) { noticeType = v; }
    public String getAudience() { return audience; } public void setAudience(String v) { audience = v; }
    public LocalDate getExpiresOn() { return expiresOn; } public void setExpiresOn(LocalDate v) { expiresOn = v; }
    public Long getCreatedById() { return createdById; } public void setCreatedById(Long v) { createdById = v; }
    public String getCreatedByName() { return createdByName; } public void setCreatedByName(String v) { createdByName = v; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; } public void setUpdatedAt(LocalDateTime v) { updatedAt = v; }
}