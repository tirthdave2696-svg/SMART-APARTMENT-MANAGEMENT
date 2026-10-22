package com.example.demo.model;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "complaints")
public class Complaint {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long residentId;
    private String residentName;
    private String unitNumber;
    @Column(nullable = false, length = 1000) private String description;
    @Column(nullable = false, length = 40) private String category;
    private LocalDate complaintDate;
    @Column(nullable = false, length = 20) private String status = "OPEN";
    private Long assignedToId;
    private String assignedToName;
    @Column(length = 500) private String resolutionNote;
    private LocalDateTime createdAt = LocalDateTime.now();
    private LocalDateTime updatedAt = LocalDateTime.now();

    public Long getId() { return id; }
    public Long getResidentId() { return residentId; } public void setResidentId(Long v) { residentId = v; }
    public String getResidentName() { return residentName; } public void setResidentName(String v) { residentName = v; }
    public String getUnitNumber() { return unitNumber; } public void setUnitNumber(String v) { unitNumber = v; }
    public String getDescription() { return description; } public void setDescription(String v) { description = v; }
    public String getCategory() { return category; } public void setCategory(String v) { category = v; }
    public LocalDate getComplaintDate() { return complaintDate; } public void setComplaintDate(LocalDate v) { complaintDate = v; }
    public String getStatus() { return status; } public void setStatus(String v) { status = v; }
    public Long getAssignedToId() { return assignedToId; } public void setAssignedToId(Long v) { assignedToId = v; }
    public String getAssignedToName() { return assignedToName; } public void setAssignedToName(String v) { assignedToName = v; }
    public String getResolutionNote() { return resolutionNote; } public void setResolutionNote(String v) { resolutionNote = v; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; } public void setUpdatedAt(LocalDateTime v) { updatedAt = v; }
}