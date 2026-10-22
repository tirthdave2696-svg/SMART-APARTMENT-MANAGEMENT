package com.example.demo.dto;

import com.example.demo.model.VisitRequest;
import com.example.demo.model.VisitStatus;
import jakarta.validation.constraints.*;
import java.time.LocalDateTime;

public final class VisitDtos {
    private VisitDtos() {}

    public record GateVisitorRequest(
            @NotBlank String fullName,
            @NotBlank @Pattern(regexp = Dtos.PHONE, message = "must be 10-13 digits") String phone,
            @NotBlank String purpose,
            @NotBlank String unitNumber,
            String residentName, String vehicleNumber, String idProof) {}

    public record PreRegisterRequest(
            @NotBlank String visitorName,
            @NotBlank String purpose,
            @NotNull LocalDateTime expectedDateTime,
            @Pattern(regexp = Dtos.PHONE, message = "must be 10-13 digits") String phone,
            String vehicleNumber) {}

    public record UpdateVisitRequest(
            String visitorName,
            @Pattern(regexp = Dtos.PHONE, message = "must be 10-13 digits") String phone,
            String purpose, String vehicleNumber, String idProof, String reason) {}

    public record OverrideRequest(@NotBlank String decision, @NotBlank String reason) {}

    public record TimeCorrectionRequest(@NotNull LocalDateTime timestamp, @NotBlank String reason) {}

    public record VisitStatusView(Long id, String visitorName, String unitNumber, String status, boolean entryAllowed) {
        public static VisitStatusView from(VisitRequest v) {
            return new VisitStatusView(v.getId(), v.getVisitorName(), v.getUnitNumber(), v.getStatus(),
                    VisitStatus.APPROVED.equals(v.getStatus()));
        }
    }

    public record VisitResponse(Long id, String visitorName, String phone, String purpose, Long residentId,
                                String residentName, String unitNumber, LocalDateTime visitDateTime,
                                String vehicleNumber, String idProof, String status, String gateStatus,
                                boolean preRegistered, boolean followUpRequired, LocalDateTime decidedAt,
                                String decisionReason, boolean overridden, LocalDateTime entryTime,
                                LocalDateTime exitTime, Long durationMinutes, LocalDateTime createdAt) {
        public static VisitResponse from(VisitRequest v) {
            boolean followUp = VisitStatus.PENDING.equals(v.getStatus())
                    && v.getCreatedAt() != null && v.getCreatedAt().plusMinutes(30).isBefore(LocalDateTime.now());
            String gate;
            switch (v.getStatus()) {
                case VisitStatus.PENDING -> gate = "Awaiting resident approval";
                case VisitStatus.PRE_REGISTERED -> gate = "Pre-registered, not yet arrived";
                case VisitStatus.APPROVED -> gate = "Not yet entered";
                case VisitStatus.REJECTED -> gate = "Entry denied";
                case VisitStatus.INSIDE -> gate = "Inside premises";
                case VisitStatus.EXITED -> gate = "Exited";
                default -> gate = v.getStatus();
            }
            return new VisitResponse(v.getId(), v.getVisitorName(), v.getPhone(), v.getPurpose(), v.getResidentId(),
                    v.getResidentName(), v.getUnitNumber(), v.getVisitDateTime(), v.getVehicleNumber(),
                    v.getIdProof(), v.getStatus(), gate, v.isPreRegistered(), followUp, v.getDecidedAt(),
                    v.getDecisionReason(), v.isOverridden(), v.getEntryTime(), v.getExitTime(),
                    v.getDurationMinutes(), v.getCreatedAt());
        }
    }
}