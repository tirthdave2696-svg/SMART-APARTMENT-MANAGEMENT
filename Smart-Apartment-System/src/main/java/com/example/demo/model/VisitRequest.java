package com.example.demo.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "visit_requests")
public class VisitRequest {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String visitorName;
    private String phone;
    private String purpose;
    private Long residentId;
    private String residentName;
    private String unitNumber;
    private LocalDateTime visitDateTime;
    private String vehicleNumber;
    private String idProof;
    private String status = VisitStatus.PENDING;
    private LocalDateTime createdAt = LocalDateTime.now();

    private Long registeredById;
    private boolean preRegistered = false;
    private Long decidedById;
    private LocalDateTime decidedAt;
    @Column(length = 500) private String decisionReason;
    private boolean overridden = false;
    private LocalDateTime entryTime;
    private Long entryRecordedById;
    private LocalDateTime exitTime;
    private Long exitRecordedById;
    private Long durationMinutes;

    public Long getId() { return id; }
    public String getVisitorName() { return visitorName; } public void setVisitorName(String v) { visitorName = v; }
    public String getPhone() { return phone; } public void setPhone(String v) { phone = v; }
    public String getPurpose() { return purpose; } public void setPurpose(String v) { purpose = v; }
    public Long getResidentId() { return residentId; } public void setResidentId(Long v) { residentId = v; }
    public String getResidentName() { return residentName; } public void setResidentName(String v) { residentName = v; }
    public String getUnitNumber() { return unitNumber; } public void setUnitNumber(String v) { unitNumber = v; }
    public LocalDateTime getVisitDateTime() { return visitDateTime; } public void setVisitDateTime(LocalDateTime v) { visitDateTime = v; }
    public String getVehicleNumber() { return vehicleNumber; } public void setVehicleNumber(String v) { vehicleNumber = v; }
    public String getIdProof() { return idProof; } public void setIdProof(String v) { idProof = v; }
    public String getStatus() { return status; } public void setStatus(String v) { status = v; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public Long getRegisteredById() { return registeredById; } public void setRegisteredById(Long v) { registeredById = v; }
    public boolean isPreRegistered() { return preRegistered; } public void setPreRegistered(boolean v) { preRegistered = v; }
    public Long getDecidedById() { return decidedById; } public void setDecidedById(Long v) { decidedById = v; }
    public LocalDateTime getDecidedAt() { return decidedAt; } public void setDecidedAt(LocalDateTime v) { decidedAt = v; }
    public String getDecisionReason() { return decisionReason; } public void setDecisionReason(String v) { decisionReason = v; }
    public boolean isOverridden() { return overridden; } public void setOverridden(boolean v) { overridden = v; }
    public LocalDateTime getEntryTime() { return entryTime; } public void setEntryTime(LocalDateTime v) { entryTime = v; }
    public Long getEntryRecordedById() { return entryRecordedById; } public void setEntryRecordedById(Long v) { entryRecordedById = v; }
    public LocalDateTime getExitTime() { return exitTime; } public void setExitTime(LocalDateTime v) { exitTime = v; }
    public Long getExitRecordedById() { return exitRecordedById; } public void setExitRecordedById(Long v) { exitRecordedById = v; }
    public Long getDurationMinutes() { return durationMinutes; } public void setDurationMinutes(Long v) { durationMinutes = v; }
}