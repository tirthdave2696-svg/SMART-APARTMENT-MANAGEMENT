package com.example.demo.service;

import com.example.demo.dto.VisitDtos.*;
import com.example.demo.exception.ApiException;
import com.example.demo.model.*;
import com.example.demo.repository.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class VisitService {
    private final VisitRequestRepository visits;
    private final UserRepository users;
    private final UnitRepository units;
    private final AuditService audit;

    public VisitService(VisitRequestRepository visits, UserRepository users, UnitRepository units, AuditService audit) {
        this.visits = visits; this.users = users; this.units = units; this.audit = audit;
    }

    // ------------------------------------------------------------ helpers

    private VisitRequest find(Long id) {
        return visits.findById(id).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Visit request not found"));
    }

    private static void own(User actor, VisitRequest v) {
        if (actor.getRole() == Role.RESIDENT && !actor.getId().equals(v.getResidentId()))
            throw new ApiException(HttpStatus.FORBIDDEN, "You can only access your own visitors");
    }

    private static String reason(String r) {
        if (r == null || r.isBlank()) throw new ApiException(HttpStatus.BAD_REQUEST, "A reason is required");
        return r.trim();
    }

    private static boolean has(String s) { return s != null && !s.isBlank(); }

    private List<VisitRequest> scoped(User actor) {
        return actor.getRole() == Role.RESIDENT
                ? visits.findByResidentIdOrderByCreatedAtDesc(actor.getId())
                : visits.findAllByOrderByCreatedAtDesc();
    }

    private User findResident(String unitNumber, String name) {
        String unit = unitNumber.trim().toUpperCase();
        if (!units.existsById(unit))
            throw new ApiException(HttpStatus.BAD_REQUEST, "Unit number is not recognized");
        List<User> residents = users.findByRoleAndUnitNumberAndStatus(Role.RESIDENT, unit, AccountStatus.ACTIVE);
        if (residents.isEmpty())
            throw new ApiException(HttpStatus.NOT_FOUND, "No active resident is registered in unit " + unit);
        if (has(name))
            return residents.stream().filter(r -> r.getFullName().equalsIgnoreCase(name.trim())).findFirst()
                    .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND,
                            "No active resident with that name lives in this unit"));
        if (residents.size() > 1)
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "More than one resident lives in this unit; please enter the resident name");
        return residents.get(0);
    }

    private void notifyResident(Long residentId, String message) {
        users.findById(residentId).ifPresent(r -> audit.notifyUser(r, message));
    }

    // ------------------------------------------------------------ T05 Register visitor

    @Transactional
    public VisitRequest registerAtGate(User actor, GateVisitorRequest d) {
        User resident = findResident(d.unitNumber(), d.residentName());
        VisitRequest v = new VisitRequest();
        v.setVisitorName(d.fullName().trim());
        v.setPhone(d.phone().trim());
        v.setPurpose(d.purpose().trim());
        v.setResidentId(resident.getId());
        v.setResidentName(resident.getFullName());
        v.setUnitNumber(resident.getUnitNumber());
        v.setVisitDateTime(LocalDateTime.now());
        v.setVehicleNumber(has(d.vehicleNumber()) ? d.vehicleNumber().trim() : null);
        v.setIdProof(has(d.idProof()) ? d.idProof().trim() : null);
        v.setRegisteredById(actor.getId());
        v.setStatus(VisitStatus.PENDING);
        visits.save(v);
        audit.log(actor.getId(), "VISIT_REGISTER", resident.getId(), "visit " + v.getId() + " " + v.getVisitorName());
        audit.notifyUser(resident, "Visitor " + v.getVisitorName() + " is at the gate (" + v.getPurpose() + "). Please approve or reject.");
        return v;
    }

    @Transactional
    public VisitRequest preRegister(User resident, PreRegisterRequest d) {
        if (!has(resident.getUnitNumber()))
            throw new ApiException(HttpStatus.BAD_REQUEST, "Your account is not linked to a unit");
        if (d.expectedDateTime().isBefore(LocalDateTime.now().minusMinutes(5)))
            throw new ApiException(HttpStatus.BAD_REQUEST, "expectedDateTime: cannot be in the past");
        VisitRequest v = new VisitRequest();
        v.setVisitorName(d.visitorName().trim());
        v.setPurpose(d.purpose().trim());
        v.setPhone(has(d.phone()) ? d.phone().trim() : null);
        v.setVehicleNumber(has(d.vehicleNumber()) ? d.vehicleNumber().trim() : null);
        v.setVisitDateTime(d.expectedDateTime());
        v.setResidentId(resident.getId());
        v.setResidentName(resident.getFullName());
        v.setUnitNumber(resident.getUnitNumber());
        v.setRegisteredById(resident.getId());
        v.setPreRegistered(true);
        v.setStatus(VisitStatus.PRE_REGISTERED);
        visits.save(v);
        audit.log(resident.getId(), "VISIT_PREREGISTER", resident.getId(), "visit " + v.getId() + " " + v.getVisitorName());
        return v;
    }

    @Transactional(readOnly = true)
    public List<VisitResponse> list(User actor, String status) {
        return scoped(actor).stream()
                .filter(v -> !has(status) || v.getStatus().equalsIgnoreCase(status.trim()))
                .map(VisitResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public VisitResponse get(User actor, Long id) {
        VisitRequest v = find(id);
        own(actor, v);
        return VisitResponse.from(v);
    }

    @Transactional(readOnly = true)
    public List<VisitResponse> preRegisteredList() {
        return visits.findAllByOrderByCreatedAtDesc().stream()
                .filter(v -> VisitStatus.PRE_REGISTERED.equals(v.getStatus()))
                .map(VisitResponse::from).toList();
    }

    /** Guard confirms a pre-registered visitor has arrived. The resident pre-authorised the visit, so it becomes APPROVED. */
    @Transactional
    public VisitResponse confirmArrival(User guard, Long id) {
        VisitRequest v = find(id);
        if (!VisitStatus.PRE_REGISTERED.equals(v.getStatus()))
            throw new ApiException(HttpStatus.CONFLICT, "Only pre-registered visitors can be confirmed (current status: " + v.getStatus() + ")");
        v.setStatus(VisitStatus.APPROVED);
        v.setDecidedById(v.getResidentId());
        v.setDecidedAt(LocalDateTime.now());
        v.setDecisionReason("Pre-approved by resident; arrival confirmed by guard");
        visits.save(v);
        audit.log(guard.getId(), "VISIT_CONFIRM_ARRIVAL", v.getResidentId(), "visit " + id);
        return VisitResponse.from(v);
    }

    @Transactional
    public VisitResponse update(User actor, Long id, UpdateVisitRequest d) {
        VisitRequest v = find(id);
        if (actor.getRole() == Role.SECURITY_GUARD) {
            if (!VisitStatus.PENDING.equals(v.getStatus()) && !VisitStatus.PRE_REGISTERED.equals(v.getStatus()))
                throw new ApiException(HttpStatus.FORBIDDEN, "Security guards can edit visitor details only before approval");
        }
        String why = null;
        if (actor.getRole() == Role.ADMINISTRATOR) why = reason(d.reason());
        if (!has(d.visitorName()) && !has(d.phone()) && !has(d.purpose()) && !has(d.vehicleNumber()) && !has(d.idProof()))
            throw new ApiException(HttpStatus.BAD_REQUEST, "No changes supplied");
        if (has(d.visitorName())) v.setVisitorName(d.visitorName().trim());
        if (has(d.phone())) v.setPhone(d.phone().trim());
        if (has(d.purpose())) v.setPurpose(d.purpose().trim());
        if (has(d.vehicleNumber())) v.setVehicleNumber(d.vehicleNumber().trim());
        if (has(d.idProof())) v.setIdProof(d.idProof().trim());
        visits.save(v);
        audit.log(actor.getId(), "VISIT_EDIT", v.getResidentId(), "visit " + id + (why == null ? "" : ": " + why));
        return VisitResponse.from(v);
    }

    @Transactional
    public void delete(User admin, Long id, String why) {
        VisitRequest v = find(id);
        String r = reason(why);
        visits.delete(v);
        audit.log(admin.getId(), "VISIT_DELETE", v.getResidentId(), "visit " + id + " (" + v.getVisitorName() + "): " + r);
    }

    // ------------------------------------------------------------ T06 Approval

    @Transactional
    public VisitResponse decide(User actor, Long id, boolean approve, String why) {
        VisitRequest v = find(id);
        own(actor, v);
        if (!VisitStatus.PENDING.equals(v.getStatus()))
            throw new ApiException(HttpStatus.CONFLICT,
                    "Only pending requests can be approved or rejected (current status: " + v.getStatus() + ")");
        v.setStatus(approve ? VisitStatus.APPROVED : VisitStatus.REJECTED);
        v.setDecidedById(actor.getId());
        v.setDecidedAt(LocalDateTime.now());
        v.setDecisionReason(has(why) ? why.trim() : null);
        visits.save(v);
        audit.log(actor.getId(), approve ? "VISIT_APPROVE" : "VISIT_REJECT", v.getResidentId(),
                "visit " + id + (has(why) ? ": " + why.trim() : ""));
        return VisitResponse.from(v);   // guard sees the new status immediately via the API
    }

    @Transactional
    public VisitResponse override(User admin, Long id, String decision, String why) {
        VisitRequest v = find(id);
        String r = reason(why);
        boolean approve;
        if ("APPROVE".equalsIgnoreCase(decision)) approve = true;
        else if ("REJECT".equalsIgnoreCase(decision)) approve = false;
        else throw new ApiException(HttpStatus.BAD_REQUEST, "decision must be APPROVE or REJECT");
        if (VisitStatus.INSIDE.equals(v.getStatus()) || VisitStatus.EXITED.equals(v.getStatus()))
            throw new ApiException(HttpStatus.CONFLICT, "Cannot override a visit that has already started");
        String target = approve ? VisitStatus.APPROVED : VisitStatus.REJECTED;
        if (target.equals(v.getStatus()))
            throw new ApiException(HttpStatus.CONFLICT, "Visit is already " + target);
        v.setStatus(target);
        v.setOverridden(true);
        v.setDecidedById(admin.getId());
        v.setDecidedAt(LocalDateTime.now());
        v.setDecisionReason(r);
        visits.save(v);
        audit.log(admin.getId(), "VISIT_OVERRIDE", v.getResidentId(), "visit " + id + " -> " + target + ": " + r);
        notifyResident(v.getResidentId(), "The Administrator changed visitor " + v.getVisitorName() + " to " + target);
        return VisitResponse.from(v);
    }

    @Transactional(readOnly = true)
    public List<?> pending(User actor) {
        return scoped(actor).stream()
                .filter(v -> VisitStatus.PENDING.equals(v.getStatus()))
                .map(v -> actor.getRole() == Role.SECURITY_GUARD ? (Object) VisitStatusView.from(v) : (Object) VisitResponse.from(v))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<VisitResponse> history(User actor) {
        return scoped(actor).stream().filter(v -> v.getDecidedAt() != null).map(VisitResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public VisitStatusView status(User actor, Long id) {
        VisitRequest v = find(id);
        own(actor, v);
        return VisitStatusView.from(v);
    }

    // ------------------------------------------------------------ T07 Entry / T08 Exit

    @Transactional
    public VisitResponse recordEntry(User guard, Long id) {
        VisitRequest v = find(id);
        if (VisitStatus.INSIDE.equals(v.getStatus()))
            throw new ApiException(HttpStatus.CONFLICT, "Visitor is already inside the premises");
        if (!VisitStatus.APPROVED.equals(v.getStatus()))
            throw new ApiException(HttpStatus.CONFLICT,
                    "Not approved: entry cannot be recorded (current status: " + v.getStatus() + ")");
        v.setEntryTime(LocalDateTime.now());   // server clock, never typed by a user
        v.setEntryRecordedById(guard.getId());
        v.setStatus(VisitStatus.INSIDE);
        visits.save(v);
        audit.log(guard.getId(), "VISIT_ENTRY", v.getResidentId(), "visit " + id);
        notifyResident(v.getResidentId(), "Your visitor " + v.getVisitorName() + " has entered the premises.");
        return VisitResponse.from(v);
    }

    @Transactional
    public VisitResponse recordExit(User guard, Long id) {
        VisitRequest v = find(id);
        if (!VisitStatus.INSIDE.equals(v.getStatus()) || v.getEntryTime() == null)
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "Exit cannot be recorded: this visitor has no recorded entry or has already exited");
        LocalDateTime now = LocalDateTime.now();
        v.setExitTime(now);
        v.setExitRecordedById(guard.getId());
        v.setDurationMinutes(Duration.between(v.getEntryTime(), now).toMinutes());
        v.setStatus(VisitStatus.EXITED);
        visits.save(v);
        audit.log(guard.getId(), "VISIT_EXIT", v.getResidentId(), "visit " + id);
        notifyResident(v.getResidentId(), "Your visitor " + v.getVisitorName() + " has left the premises.");
        return VisitResponse.from(v);
    }

    @Transactional(readOnly = true)
    public List<VisitResponse> logs(User actor, String status) {
        return scoped(actor).stream()
                .filter(v -> VisitStatus.APPROVED.equals(v.getStatus()) || VisitStatus.INSIDE.equals(v.getStatus())
                        || VisitStatus.EXITED.equals(v.getStatus()))
                .filter(v -> !has(status) || v.getStatus().equalsIgnoreCase(status.trim()))
                .map(VisitResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public List<VisitResponse> inside() {
        return visits.findAllByOrderByCreatedAtDesc().stream()
                .filter(v -> VisitStatus.INSIDE.equals(v.getStatus())).map(VisitResponse::from).toList();
    }

    // ------------------------------------------------------------ Administrator corrections

    private static void checkTimestamp(LocalDateTime ts) {
        if (ts.isAfter(LocalDateTime.now()))
            throw new ApiException(HttpStatus.BAD_REQUEST, "timestamp: cannot be in the future");
    }

    @Transactional
    public VisitResponse correctEntry(User admin, Long id, TimeCorrectionRequest d) {
        VisitRequest v = find(id);
        if (v.getEntryTime() == null) throw new ApiException(HttpStatus.BAD_REQUEST, "No entry is recorded for this visit");
        checkTimestamp(d.timestamp());
        if (v.getExitTime() != null && d.timestamp().isAfter(v.getExitTime()))
            throw new ApiException(HttpStatus.BAD_REQUEST, "Entry time cannot be after the exit time");
        LocalDateTime old = v.getEntryTime();
        v.setEntryTime(d.timestamp());
        if (v.getExitTime() != null) v.setDurationMinutes(Duration.between(v.getEntryTime(), v.getExitTime()).toMinutes());
        visits.save(v);
        audit.log(admin.getId(), "VISIT_ENTRY_CORRECT", v.getResidentId(),
                "visit " + id + ": " + old + " -> " + d.timestamp() + ": " + reason(d.reason()));
        return VisitResponse.from(v);
    }

    /** Removing an entry record also clears any exit that depended on it and returns the visit to APPROVED. */
    @Transactional
    public VisitResponse deleteEntry(User admin, Long id, String why) {
        VisitRequest v = find(id);
        if (v.getEntryTime() == null) throw new ApiException(HttpStatus.BAD_REQUEST, "No entry is recorded for this visit");
        String r = reason(why);
        v.setEntryTime(null); v.setEntryRecordedById(null);
        v.setExitTime(null); v.setExitRecordedById(null); v.setDurationMinutes(null);
        v.setStatus(VisitStatus.APPROVED);
        visits.save(v);
        audit.log(admin.getId(), "VISIT_ENTRY_DELETE", v.getResidentId(), "visit " + id + ": " + r);
        return VisitResponse.from(v);
    }

    @Transactional
    public VisitResponse correctExit(User admin, Long id, TimeCorrectionRequest d) {
        VisitRequest v = find(id);
        if (v.getExitTime() == null) throw new ApiException(HttpStatus.BAD_REQUEST, "No exit is recorded for this visit");
        checkTimestamp(d.timestamp());
        if (d.timestamp().isBefore(v.getEntryTime()))
            throw new ApiException(HttpStatus.BAD_REQUEST, "Exit time cannot be before the entry time");
        LocalDateTime old = v.getExitTime();
        v.setExitTime(d.timestamp());
        v.setDurationMinutes(Duration.between(v.getEntryTime(), v.getExitTime()).toMinutes());
        visits.save(v);
        audit.log(admin.getId(), "VISIT_EXIT_CORRECT", v.getResidentId(),
                "visit " + id + ": " + old + " -> " + d.timestamp() + ": " + reason(d.reason()));
        return VisitResponse.from(v);
    }

    @Transactional
    public VisitResponse deleteExit(User admin, Long id, String why) {
        VisitRequest v = find(id);
        if (v.getExitTime() == null) throw new ApiException(HttpStatus.BAD_REQUEST, "No exit is recorded for this visit");
        String r = reason(why);
        v.setExitTime(null); v.setExitRecordedById(null); v.setDurationMinutes(null);
        v.setStatus(VisitStatus.INSIDE);
        visits.save(v);
        audit.log(admin.getId(), "VISIT_EXIT_DELETE", v.getResidentId(), "visit " + id + ": " + r);
        return VisitResponse.from(v);
    }
}