package com.example.demo.dto;

import com.example.demo.model.*;
import jakarta.validation.constraints.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

public final class Dtos {
    private Dtos() {}

    public static final String PHONE = "^\\+?[0-9]{10,13}$";

    public record RegisterRequest(
            @NotNull(message = "is required") Role role,
            @NotBlank @Size(max = 100) String fullName,
            @NotBlank @Email String email,
            @NotBlank @Pattern(regexp = PHONE, message = "must be 10-13 digits") String phone,
            @NotBlank String password,
            String address, String unitNumber, String governmentIdProof,
            Integer numberOfOccupants, String emergencyContact, LocalDate moveInDate,
            String employeeId, String assignedGateOrBlock, String dutyShift, LocalDate joiningDate,
            String department, String designation, String assignedZone,
            LocalDate appointmentDate, String scopeOfAccess) {}

    public record LoginRequest(@NotBlank String identifier, @NotBlank String password) {}

    public record VisitorRequest(
            @NotBlank String fullName,
            @NotBlank @Pattern(regexp = PHONE, message = "must be 10-13 digits") String phone,
            @NotBlank String purpose,
            @NotBlank String residentName,
            @NotBlank String unitNumber,
            @NotNull LocalDateTime visitDateTime,
            String vehicleNumber, String idProof) {}

    public record UpdateProfileRequest(
            @Size(max = 100) String fullName,
            @Pattern(regexp = PHONE, message = "must be 10-13 digits") String phone,
            String address, String emergencyContact) {}

    public record ChangePasswordRequest(@NotBlank String currentPassword, @NotBlank String newPassword) {}

    public record DecisionRequest(String reason) {}

    public record RoleChangeRequest(@NotNull Role role, @NotBlank String reason) {}

    public record StatusChangeRequest(@NotNull AccountStatus status, @NotBlank String reason) {}

    public record CorrectionRequest(@NotBlank String message) {}

    public record SessionResponse(Long id, LocalDateTime createdAt, LocalDateTime lastAccessAt,
                                  LocalDateTime endedAt, boolean active, String endedReason, boolean current) {}

    public record UserResponse(Long id, String fullName, String email, String phone, Role role,
                               AccountStatus status, String address, String unitNumber, String employeeId,
                               String designation, String department, String assignedGateOrBlock,
                               String assignedZone, String dutyShift, LocalDateTime createdAt) {
        public static UserResponse from(User u) {
            return new UserResponse(u.getId(), u.getFullName(), u.getEmail(), u.getPhone(), u.getRole(),
                    u.getStatus(), u.getAddress(), u.getUnitNumber(), u.getEmployeeId(), u.getDesignation(),
                    u.getDepartment(), u.getAssignedGateOrBlock(), u.getAssignedZone(), u.getDutyShift(),
                    u.getCreatedAt());
        }
    }
}