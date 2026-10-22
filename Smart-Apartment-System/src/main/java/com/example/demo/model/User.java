package com.example.demo.model;


import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "users")
public class User {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, length = 100) private String fullName;
    @Column(nullable = false, unique = true, length = 150) private String email;
    @Column(nullable = false, unique = true, length = 20) private String phone;
    @Column(nullable = false) private String passwordHash;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 30) private Role role;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private AccountStatus status;

    private String address;
    private String unitNumber;
    private String governmentIdProof;
    private Integer numberOfOccupants;
    private String emergencyContact;
    private LocalDate moveInDate;
    @Column(unique = true) private String employeeId;
    private String assignedGateOrBlock;
    private String dutyShift;
    private LocalDate joiningDate;
    private String department;
    private String designation;
    private String assignedZone;
    private LocalDate appointmentDate;
    private String scopeOfAccess;

    private int failedAttempts;
    private LocalDateTime lockUntil;
    private LocalDateTime createdAt = LocalDateTime.now();

    public Long getId() { return id; }
    public String getFullName() { return fullName; } public void setFullName(String v) { fullName = v; }
    public String getEmail() { return email; } public void setEmail(String v) { email = v; }
    public String getPhone() { return phone; } public void setPhone(String v) { phone = v; }
    public String getPasswordHash() { return passwordHash; } public void setPasswordHash(String v) { passwordHash = v; }
    public Role getRole() { return role; } public void setRole(Role v) { role = v; }
    public AccountStatus getStatus() { return status; } public void setStatus(AccountStatus v) { status = v; }
    public String getAddress() { return address; } public void setAddress(String v) { address = v; }
    public String getUnitNumber() { return unitNumber; } public void setUnitNumber(String v) { unitNumber = v; }
    public String getGovernmentIdProof() { return governmentIdProof; } public void setGovernmentIdProof(String v) { governmentIdProof = v; }
    public Integer getNumberOfOccupants() { return numberOfOccupants; } public void setNumberOfOccupants(Integer v) { numberOfOccupants = v; }
    public String getEmergencyContact() { return emergencyContact; } public void setEmergencyContact(String v) { emergencyContact = v; }
    public LocalDate getMoveInDate() { return moveInDate; } public void setMoveInDate(LocalDate v) { moveInDate = v; }
    public String getEmployeeId() { return employeeId; } public void setEmployeeId(String v) { employeeId = v; }
    public String getAssignedGateOrBlock() { return assignedGateOrBlock; } public void setAssignedGateOrBlock(String v) { assignedGateOrBlock = v; }
    public String getDutyShift() { return dutyShift; } public void setDutyShift(String v) { dutyShift = v; }
    public LocalDate getJoiningDate() { return joiningDate; } public void setJoiningDate(LocalDate v) { joiningDate = v; }
    public String getDepartment() { return department; } public void setDepartment(String v) { department = v; }
    public String getDesignation() { return designation; } public void setDesignation(String v) { designation = v; }
    public String getAssignedZone() { return assignedZone; } public void setAssignedZone(String v) { assignedZone = v; }
    public LocalDate getAppointmentDate() { return appointmentDate; } public void setAppointmentDate(LocalDate v) { appointmentDate = v; }
    public String getScopeOfAccess() { return scopeOfAccess; } public void setScopeOfAccess(String v) { scopeOfAccess = v; }
    public int getFailedAttempts() { return failedAttempts; } public void setFailedAttempts(int v) { failedAttempts = v; }
    public LocalDateTime getLockUntil() { return lockUntil; } public void setLockUntil(LocalDateTime v) { lockUntil = v; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}