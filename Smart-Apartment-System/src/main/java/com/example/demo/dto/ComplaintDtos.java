package com.example.demo.dto;

import com.example.demo.model.Complaint;
import jakarta.validation.constraints.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

public final class ComplaintDtos {
    private ComplaintDtos() {}

    public record RaiseComplaintRequest(
            @NotBlank @Size(max = 1000) String description,
            @NotBlank String category,
            @NotNull LocalDate complaintDate,
            String unitNumber,
            Long residentId) {}

    public record EditComplaintRequest(@Size(max = 1000) String description, String category) {}

    public record AssignRequest(@NotNull Long staffId) {}

    public record ComplaintStatusRequest(@NotBlank String status, @Size(max = 500) String note) {}

    public record ComplaintResponse(Long id, Long residentId, String residentName, String unitNumber,
                                    String description, String category, LocalDate complaintDate, String status,
                                    Long assignedToId, String assignedToName, String resolutionNote,
                                    LocalDateTime createdAt, LocalDateTime updatedAt) {
        public static ComplaintResponse from(Complaint c) {
            return new ComplaintResponse(c.getId(), c.getResidentId(), c.getResidentName(), c.getUnitNumber(),
                    c.getDescription(), c.getCategory(), c.getComplaintDate(), c.getStatus(), c.getAssignedToId(),
                    c.getAssignedToName(), c.getResolutionNote(), c.getCreatedAt(), c.getUpdatedAt());
        }
    }

    /** Maintenance staff see the unit only, never the resident's identity. */
    public record ComplaintStaffView(Long id, String unitNumber, String description, String category,
                                     LocalDate complaintDate, String status, String resolutionNote,
                                     LocalDateTime createdAt, LocalDateTime updatedAt) {
        public static ComplaintStaffView from(Complaint c) {
            return new ComplaintStaffView(c.getId(), c.getUnitNumber(), c.getDescription(), c.getCategory(),
                    c.getComplaintDate(), c.getStatus(), c.getResolutionNote(), c.getCreatedAt(), c.getUpdatedAt());
        }
    }
}