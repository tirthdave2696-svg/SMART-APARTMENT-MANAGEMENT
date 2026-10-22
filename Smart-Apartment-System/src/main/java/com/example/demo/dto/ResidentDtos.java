package com.example.demo.dto;

import com.example.demo.model.AccountStatus;
import com.example.demo.model.User;
import jakarta.validation.constraints.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

public final class ResidentDtos {
    private ResidentDtos() {}

    public record AddResidentRequest(
            @NotBlank @Size(max = 100) String fullName,
            @NotBlank @Email String email,
            @NotBlank @Pattern(regexp = Dtos.PHONE, message = "must be 10-13 digits") String phone,
            @NotBlank String unitNumber,
            @NotBlank String governmentIdProof,
            @NotNull @Min(value = 1, message = "must be at least 1") Integer numberOfOccupants,
            String address, String emergencyContact, LocalDate moveInDate, String password) {}

    public record EditResidentRequest(
            @Size(max = 100) String fullName,
            @Email String email,
            @Pattern(regexp = Dtos.PHONE, message = "must be 10-13 digits") String phone,
            String address, String unitNumber,
            @Min(value = 1, message = "must be at least 1") Integer numberOfOccupants,
            String emergencyContact, String governmentIdProof,
            @NotBlank String reason) {}

    public record ResidentFullView(Long id, String fullName, String email, String phone, String address,
                                   String unitNumber, String governmentIdProof, Integer numberOfOccupants,
                                   String emergencyContact, LocalDate moveInDate, AccountStatus status,
                                   LocalDateTime createdAt) {
        public static ResidentFullView from(User u) {
            return new ResidentFullView(u.getId(), u.getFullName(), u.getEmail(), u.getPhone(), u.getAddress(),
                    u.getUnitNumber(), u.getGovernmentIdProof(), u.getNumberOfOccupants(), u.getEmergencyContact(),
                    u.getMoveInDate(), u.getStatus(), u.getCreatedAt());
        }
    }

    public record ResidentManagerView(Long id, String fullName, String email, String phone, String unitNumber,
                                      Integer numberOfOccupants, LocalDate moveInDate) {
        public static ResidentManagerView from(User u) {
            return new ResidentManagerView(u.getId(), u.getFullName(), u.getEmail(), u.getPhone(),
                    u.getUnitNumber(), u.getNumberOfOccupants(), u.getMoveInDate());
        }
    }

    public record ResidentBasicView(Long id, String fullName, String unitNumber) {
        public static ResidentBasicView from(User u) {
            return new ResidentBasicView(u.getId(), u.getFullName(), u.getUnitNumber());
        }
    }

    public record UnitOnlyView(String unitNumber) {}
}