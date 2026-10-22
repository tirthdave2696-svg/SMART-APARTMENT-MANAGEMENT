package com.example.demo.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "resident_edit_history")
public class EditHistory {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long residentId;
    private String fieldName;
    @Column(length = 500) private String oldValue;
    @Column(length = 500) private String newValue;
    private Long changedById;
    @Column(length = 500) private String reason;
    private LocalDateTime changedAt = LocalDateTime.now();

    public EditHistory() {}
    public EditHistory(Long residentId, String fieldName, String oldValue, String newValue, Long changedById, String reason) {
        this.residentId = residentId; this.fieldName = fieldName; this.oldValue = oldValue;
        this.newValue = newValue; this.changedById = changedById; this.reason = reason;
    }
    public Long getId() { return id; }
    public Long getResidentId() { return residentId; }
    public String getFieldName() { return fieldName; }
    public String getOldValue() { return oldValue; }
    public String getNewValue() { return newValue; }
    public Long getChangedById() { return changedById; }
    public String getReason() { return reason; }
    public LocalDateTime getChangedAt() { return changedAt; }
}