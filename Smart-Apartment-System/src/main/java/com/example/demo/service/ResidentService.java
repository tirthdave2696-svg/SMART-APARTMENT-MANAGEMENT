package com.example.demo.service;

import com.example.demo.dto.ResidentDtos.*;
import com.example.demo.exception.ApiException;
import com.example.demo.model.*;
import com.example.demo.repository.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class ResidentService {
    /** Maximum active resident accounts allowed per unit. */
    public static final int MAX_RESIDENTS_PER_UNIT = 2;
    private static final int RESTORE_DAYS = 30;
    private static final Pattern PASSWORD_POLICY =
            Pattern.compile("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z0-9]).{8,64}$");
    private static final String PASSWORD_MSG =
            "password: must be 8-64 characters with an upper-case letter, lower-case letter, digit and special character";
    private static final String ALNUM = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnpqrstuvwxyz23456789";
    private static final SecureRandom RANDOM = new SecureRandom();

    private final UserRepository users;
    private final UnitRepository units;
    private final PasswordEncoder encoder;
    private final VisitRequestRepository visits;
    private final ComplaintRepository complaints;
    private final EditHistoryRepository history;
    private final AuditLogRepository auditRepo;
    private final AuthService authService;
    private final AuditService audit;

    public ResidentService(UserRepository users, UnitRepository units, PasswordEncoder encoder,
                           VisitRequestRepository visits, ComplaintRepository complaints,
                           EditHistoryRepository history, AuditLogRepository auditRepo,
                           AuthService authService, AuditService audit) {
        this.users = users; this.units = units; this.encoder = encoder; this.visits = visits;
        this.complaints = complaints; this.history = history; this.auditRepo = auditRepo;
        this.authService = authService; this.audit = audit;
    }

    // ------------------------------------------------------------ helpers

    private User findResident(Long id) {
        return users.findById(id).filter(u -> u.getRole() == Role.RESIDENT)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Resident not found"));
    }

    private long occupancy(String unit, Long excludeId) {
        return users.findByRoleAndUnitNumberAndStatus(Role.RESIDENT, unit, AccountStatus.ACTIVE).stream()
                .filter(u -> excludeId == null || !u.getId().equals(excludeId)).count();
    }

    private List<User> activeResidents() {
        return users.findAll().stream()
                .filter(u -> u.getRole() == Role.RESIDENT && u.getStatus() == AccountStatus.ACTIVE)
                .sorted(Comparator.comparing((User u) -> u.getUnitNumber() == null ? "" : u.getUnitNumber())
                        .thenComparing(User::getFullName))
                .toList();
    }

    private static boolean has(String s) { return s != null && !s.isBlank(); }

    private static String str(Object o) { return o == null ? null : String.valueOf(o); }

    private static void track(Map<String, String[]> changes, String field, Object oldV, Object newV) {
        if (!Objects.equals(oldV, newV)) changes.put(field, new String[]{str(oldV), str(newV)});
    }

    private static String tempPassword() {
        StringBuilder sb = new StringBuilder("Tp@1a");
        for (int i = 0; i < 8; i++) sb.append(ALNUM.charAt(RANDOM.nextInt(ALNUM.length())));
        return sb.toString();
    }

    // ------------------------------------------------------------ T09 Add

    @Transactional
    public Map<String, Object> add(User admin, AddResidentRequest d) {
        String unit = d.unitNumber().trim().toUpperCase();
        String email = d.email().trim().toLowerCase();
        String phone = d.phone().trim();
        List<String> errors = new ArrayList<>();
        if (!units.existsById(unit)) errors.add("unitNumber: not a recognized apartment unit");
        if (users.existsByEmail(email)) errors.add("email: already registered");
        if (users.existsByPhone(phone)) errors.add("phone: already registered");
        if (has(d.password()) && !PASSWORD_POLICY.matcher(d.password()).matches()) errors.add(PASSWORD_MSG);
        if (!errors.isEmpty()) throw new ApiException(HttpStatus.BAD_REQUEST, "Resident not added", errors);
        if (occupancy(unit, null) >= MAX_RESIDENTS_PER_UNIT)
            throw new ApiException(HttpStatus.CONFLICT,
                    "Conflict: unit " + unit + " already has the maximum of " + MAX_RESIDENTS_PER_UNIT + " residents");

        String rawPassword = has(d.password()) ? d.password() : tempPassword();
        User u = new User();
        u.setFullName(d.fullName().trim());
        u.setEmail(email);
        u.setPhone(phone);
        u.setPasswordHash(encoder.encode(rawPassword));
        u.setRole(Role.RESIDENT);
        u.setStatus(AccountStatus.ACTIVE);
        u.setUnitNumber(unit);
        u.setGovernmentIdProof(d.governmentIdProof().trim());
        u.setNumberOfOccupants(d.numberOfOccupants());
        u.setAddress(has(d.address()) ? d.address().trim() : null);
        u.setEmergencyContact(has(d.emergencyContact()) ? d.emergencyContact().trim() : null);
        u.setMoveInDate(d.moveInDate());
        users.save(u);
        audit.log(admin.getId(), "RESIDENT_ADD", u.getId(), u.getFullName() + " (unit " + unit + ")");

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("message", "Resident added and linked to unit " + unit);
        out.put("resident", ResidentFullView.from(u));
        if (!has(d.password())) {
            out.put("temporaryPassword", rawPassword);
            out.put("note", "Shown once only. Give it to the resident and ask them to change it after login.");
        }
        return out;
    }

    // ------------------------------------------------------------ T10 View

    @Transactional(readOnly = true)
    public List<Object> directory(User actor, String unit, String name, String block) {
        return activeResidents().stream()
                .filter(u -> !has(unit) || (u.getUnitNumber() != null && u.getUnitNumber().equalsIgnoreCase(unit.trim())))
                .filter(u -> !has(name) || u.getFullName().toLowerCase().contains(name.trim().toLowerCase()))
                .filter(u -> !has(block) || (u.getUnitNumber() != null
                        && u.getUnitNumber().toUpperCase().startsWith(block.trim().toUpperCase() + "-")))
                .map(u -> shape(actor, u)).toList();
    }

    private Object shape(User actor, User u) {
        return switch (actor.getRole()) {
            case ADMINISTRATOR -> ResidentFullView.from(u);
            case APARTMENT_MANAGER -> ResidentManagerView.from(u);
            default -> ResidentBasicView.from(u);   // security guard: name and unit only
        };
    }

    @Transactional(readOnly = true)
    public ResidentFullView myProfile(User actor) {
        User u = users.findById(actor.getId()).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Profile not found"));
        return ResidentFullView.from(u);
    }

    @Transactional(readOnly = true)
    public Object get(User actor, Long id) {
        User r = findResident(id);
        switch (actor.getRole()) {
            case ADMINISTRATOR:
                return ResidentFullView.from(r);
            case RESIDENT:
                if (!actor.getId().equals(id))
                    throw new ApiException(HttpStatus.FORBIDDEN, "You can only view your own profile");
                return ResidentFullView.from(r);
            default:
                if (r.getStatus() != AccountStatus.ACTIVE)
                    throw new ApiException(HttpStatus.NOT_FOUND, "Resident not found");
                return ResidentManagerView.from(r);
        }
    }

    @Transactional(readOnly = true)
    public UnitOnlyView unitForComplaint(User staff, String unit) {
        String u = unit.trim().toUpperCase();
        if (!complaints.existsByAssignedToIdAndUnitNumber(staff.getId(), u))
            throw new ApiException(HttpStatus.FORBIDDEN, "You can only view units linked to your assigned complaints");
        return new UnitOnlyView(u);
    }

    @Transactional(readOnly = true)
    public String exportCsv(User actor) {
        boolean admin = actor.getRole() == Role.ADMINISTRATOR;
        StringBuilder sb = new StringBuilder(admin
                ? "Name,Unit,Occupants,Phone,Email,MoveInDate,Address,EmergencyContact,GovernmentId\n"
                : "Name,Unit,Occupants,Phone,Email,MoveInDate\n");
        for (User u : activeResidents()) {
            sb.append(csv(u.getFullName())).append(',').append(csv(u.getUnitNumber())).append(',')
              .append(csv(str(u.getNumberOfOccupants()))).append(',').append(csv(u.getPhone())).append(',')
              .append(csv(u.getEmail())).append(',').append(csv(str(u.getMoveInDate())));
            if (admin) sb.append(',').append(csv(u.getAddress())).append(',').append(csv(u.getEmergencyContact()))
                         .append(',').append(csv(u.getGovernmentIdProof()));
            sb.append('\n');
        }
        return sb.toString();
    }

    private static String csv(String s) {
        if (s == null) return "\"\"";
        return "\"" + s.replace("\"", "\"\"") + "\"";
    }

    @Transactional(readOnly = true)
    public List<String> vacantUnits() {
        Set<String> occupied = activeResidents().stream().map(User::getUnitNumber).collect(Collectors.toSet());
        return units.findAll().stream().map(Unit::getUnitNumber).filter(x -> !occupied.contains(x)).sorted().toList();
    }

    // ------------------------------------------------------------ T11 Edit

    @Transactional
    public ResidentFullView edit(User admin, Long id, EditResidentRequest d) {
        User r = findResident(id);
        if (r.getStatus() == AccountStatus.DELETED)
            throw new ApiException(HttpStatus.CONFLICT, "This resident record has been deleted");
        List<String> errors = new ArrayList<>();

        String newName = has(d.fullName()) ? d.fullName().trim() : r.getFullName();
        String newEmail = has(d.email()) ? d.email().trim().toLowerCase() : r.getEmail();
        String newPhone = has(d.phone()) ? d.phone().trim() : r.getPhone();
        String newAddress = has(d.address()) ? d.address().trim() : r.getAddress();
        String newEmergency = has(d.emergencyContact()) ? d.emergencyContact().trim() : r.getEmergencyContact();
        String newIdProof = has(d.governmentIdProof()) ? d.governmentIdProof().trim() : r.getGovernmentIdProof();
        Integer newOcc = d.numberOfOccupants() != null ? d.numberOfOccupants() : r.getNumberOfOccupants();
        String newUnit = has(d.unitNumber()) ? d.unitNumber().trim().toUpperCase() : r.getUnitNumber();

        if (!newEmail.equals(r.getEmail()) && users.existsByEmail(newEmail)) errors.add("email: already registered");
        if (!newPhone.equals(r.getPhone()) && users.existsByPhone(newPhone)) errors.add("phone: already registered");
        if (!Objects.equals(newUnit, r.getUnitNumber())) {
            if (!units.existsById(newUnit))
                errors.add("unitNumber: not a valid unit, please enter a valid unit number");
            else if (occupancy(newUnit, id) >= MAX_RESIDENTS_PER_UNIT)
                errors.add("unitNumber: unit " + newUnit + " is already fully assigned, please enter a valid unit number");
        }
        if (!errors.isEmpty()) throw new ApiException(HttpStatus.BAD_REQUEST, "Edit rejected", errors);

        Map<String, String[]> changes = new LinkedHashMap<>();
        track(changes, "fullName", r.getFullName(), newName);
        track(changes, "email", r.getEmail(), newEmail);
        track(changes, "phone", r.getPhone(), newPhone);
        track(changes, "address", r.getAddress(), newAddress);
        track(changes, "emergencyContact", r.getEmergencyContact(), newEmergency);
        track(changes, "governmentIdProof", r.getGovernmentIdProof(), newIdProof);
        track(changes, "numberOfOccupants", r.getNumberOfOccupants(), newOcc);
        track(changes, "unitNumber", r.getUnitNumber(), newUnit);
        if (changes.isEmpty()) throw new ApiException(HttpStatus.BAD_REQUEST, "No changes supplied");

        r.setFullName(newName); r.setEmail(newEmail); r.setPhone(newPhone); r.setAddress(newAddress);
        r.setEmergencyContact(newEmergency); r.setGovernmentIdProof(newIdProof);
        r.setNumberOfOccupants(newOcc); r.setUnitNumber(newUnit);
        users.save(r);

        String why = d.reason().trim();
        changes.forEach((field, vals) ->
                history.save(new EditHistory(id, field, vals[0], vals[1], admin.getId(), why)));

        if (changes.containsKey("unitNumber") || changes.containsKey("fullName")) {
            visits.findByResidentId(id).forEach(v -> {
                v.setUnitNumber(newUnit); v.setResidentName(newName); visits.save(v);
            });
            complaints.findByResidentId(id).forEach(c -> {
                c.setUnitNumber(newUnit); c.setResidentName(newName); complaints.save(c);
            });
        }
        audit.log(admin.getId(), "RESIDENT_EDIT", id, String.join(",", changes.keySet()) + ": " + why);
        return ResidentFullView.from(r);
    }

    @Transactional(readOnly = true)
    public List<EditHistory> history(User actor, Long id) {
        if (actor.getRole() == Role.RESIDENT && !actor.getId().equals(id))
            throw new ApiException(HttpStatus.FORBIDDEN, "You can only view the history of your own record");
        findResident(id);
        return history.findByResidentIdOrderByChangedAtDesc(id);
    }

    // ------------------------------------------------------------ T12 Delete / restore

    @Transactional
    public void notifyRemoval(User admin, Long id) {
        User r = findResident(id);
        if (r.getStatus() != AccountStatus.ACTIVE)
            throw new ApiException(HttpStatus.CONFLICT, "Only active residents can be notified");
        audit.notifyUser(r, "Your resident record is scheduled for removal. Contact the Administrator if this is a mistake.");
        audit.log(admin.getId(), "RESIDENT_REMOVAL_NOTICE", id, null);
    }

    @Transactional
    public void delete(User admin, Long id, String reason, boolean confirmMoveOut) {
        if (!confirmMoveOut)
            throw new ApiException(HttpStatus.BAD_REQUEST, "Confirmation of move-out is required (confirmMoveOut=true)");
        if (!has(reason)) throw new ApiException(HttpStatus.BAD_REQUEST, "A reason for deletion is required");
        User r = findResident(id);
        if (r.getStatus() == AccountStatus.DELETED)
            throw new ApiException(HttpStatus.CONFLICT, "This resident record is already deleted");
        r.setStatus(AccountStatus.DELETED);   // soft delete: kept for audit, login revoked
        users.save(r);
        authService.revokeAllSessions(id, "DELETED");

        LocalDateTime now = LocalDateTime.now();
        visits.findByResidentId(id).forEach(v -> {
            if (VisitStatus.PENDING.equals(v.getStatus()) || VisitStatus.PRE_REGISTERED.equals(v.getStatus())) {
                v.setStatus(VisitStatus.REJECTED);
                v.setDecidedAt(now);
                v.setDecisionReason("Resident record removed");
                visits.save(v);
            }
        });
        audit.log(admin.getId(), "RESIDENT_DELETE", id, r.getFullName() + " (unit " + r.getUnitNumber() + "): " + reason.trim());
        audit.notifyUser(r, "Your resident record has been removed.");
    }

    @Transactional
    public ResidentFullView restore(User admin, Long id) {
        User r = findResident(id);
        if (r.getStatus() != AccountStatus.DELETED)
            throw new ApiException(HttpStatus.CONFLICT, "Only deleted residents can be restored");
        AuditLog last = auditRepo.findByActionOrderByCreatedAtDesc("RESIDENT_DELETE").stream()
                .filter(a -> id.equals(a.getTargetUserId())).findFirst()
                .orElseThrow(() -> new ApiException(HttpStatus.CONFLICT, "No deletion record found for this resident"));
        if (last.getCreatedAt().plusDays(RESTORE_DAYS).isBefore(LocalDateTime.now()))
            throw new ApiException(HttpStatus.CONFLICT, "The " + RESTORE_DAYS + "-day restore period has expired");
        if (r.getUnitNumber() != null && occupancy(r.getUnitNumber(), id) >= MAX_RESIDENTS_PER_UNIT)
            throw new ApiException(HttpStatus.CONFLICT, "Unit " + r.getUnitNumber() + " has since been reassigned and is full");
        r.setStatus(AccountStatus.ACTIVE);
        users.save(r);
        audit.log(admin.getId(), "RESIDENT_RESTORE", id, null);
        audit.notifyUser(r, "Your resident record has been restored.");
        return ResidentFullView.from(r);
    }

    @Transactional(readOnly = true)
    public List<AuditLog> deletionHistory() { return audit.list("RESIDENT_DELETE"); }
}