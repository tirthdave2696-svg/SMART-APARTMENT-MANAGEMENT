package com.example.demo.service;

import com.example.demo.dto.Dtos.*;
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

@Service
public class AuthService {

    public record AuthContext(User user, UserSession session) {}

    private static final int MAX_ATTEMPTS = 5;
    private static final int LOCK_MINUTES = 15;
    private static final int IDLE_MINUTES = 30;
    private static final String INVALID = "Invalid credentials";
    private static final Pattern PASSWORD_POLICY =
            Pattern.compile("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z0-9]).{8,64}$");
    private static final String PASSWORD_MSG =
            "password: must be 8-64 characters with an upper-case letter, lower-case letter, digit and special character";
    private static final SecureRandom RANDOM = new SecureRandom();

    private final UserRepository users;
    private final UserSessionRepository sessions;
    private final UnitRepository units;
    private final VisitRequestRepository visits;
    private final PasswordEncoder encoder;
    private final AuditService audit;

    public AuthService(UserRepository users, UserSessionRepository sessions, UnitRepository units,
                       VisitRequestRepository visits, PasswordEncoder encoder, AuditService audit) {
        this.users = users; this.sessions = sessions; this.units = units;
        this.visits = visits; this.encoder = encoder; this.audit = audit;
    }

    // ---------------------------------------------------------------- US01 Register

    @Transactional
    public User register(RegisterRequest r) {
        List<String> errors = new ArrayList<>();
        if (!PASSWORD_POLICY.matcher(r.password()).matches()) errors.add(PASSWORD_MSG);
        validateRoleFields(r, errors);

        String email = r.email().trim().toLowerCase();
        String phone = r.phone().trim();
        String employeeId = clean(r.employeeId());
        if (users.existsByEmail(email)) errors.add("email: already registered");
        if (users.existsByPhone(phone)) errors.add("phone: already registered");
        if (employeeId != null && users.existsByEmployeeId(employeeId)) errors.add("employeeId: already registered");
        if (!errors.isEmpty()) throw new ApiException(HttpStatus.BAD_REQUEST, "Registration rejected", errors);

        User u = new User();
        u.setFullName(r.fullName().trim());
        u.setEmail(email);
        u.setPhone(phone);
        u.setPasswordHash(encoder.encode(r.password()));   // one-way hash, never stored in plain text
        u.setRole(r.role());
        u.setAddress(clean(r.address()));
        String unit = clean(r.unitNumber());
        u.setUnitNumber(unit == null ? null : unit.toUpperCase());
        u.setGovernmentIdProof(clean(r.governmentIdProof()));
        u.setNumberOfOccupants(r.numberOfOccupants());
        u.setEmergencyContact(clean(r.emergencyContact()));
        u.setMoveInDate(r.moveInDate());
        u.setEmployeeId(employeeId);
        u.setAssignedGateOrBlock(clean(r.assignedGateOrBlock()));
        u.setDutyShift(clean(r.dutyShift()));
        u.setJoiningDate(r.joiningDate());
        u.setDepartment(clean(r.department()));
        u.setDesignation(clean(r.designation()));
        u.setAssignedZone(clean(r.assignedZone()));
        u.setAppointmentDate(r.appointmentDate());
        u.setScopeOfAccess(clean(r.scopeOfAccess()));

        switch (r.role()) {
            case GUEST, RESIDENT -> u.setStatus(AccountStatus.ACTIVE);
            // the very first Administrator (installation) is active; later ones need approval
            case ADMINISTRATOR -> u.setStatus(users.existsByRole(Role.ADMINISTRATOR)
                    ? AccountStatus.PENDING : AccountStatus.ACTIVE);
            default -> u.setStatus(AccountStatus.PENDING);
        }
        users.save(u);
        audit.log(u.getId(), "REGISTER", u.getId(), u.getRole() + " -> " + u.getStatus());
        return u;
    }

    @Transactional
    public VisitRequest registerVisitor(VisitorRequest d) {
        String unit = d.unitNumber().trim().toUpperCase();
        if (!units.existsById(unit))
            throw new ApiException(HttpStatus.BAD_REQUEST, "Unit number is not recognized");
        User resident = users.findByRoleAndUnitNumberAndStatus(Role.RESIDENT, unit, AccountStatus.ACTIVE).stream()
                .filter(x -> x.getFullName().equalsIgnoreCase(d.residentName().trim()))
                .findFirst()
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND,
                        "No active resident with that name lives in this unit"));

        VisitRequest v = new VisitRequest();
        v.setVisitorName(d.fullName().trim());
        v.setPhone(d.phone().trim());
        v.setPurpose(d.purpose().trim());
        v.setResidentId(resident.getId());
        v.setResidentName(resident.getFullName());
        v.setUnitNumber(unit);
        v.setVisitDateTime(d.visitDateTime());
        v.setVehicleNumber(clean(d.vehicleNumber()));
        v.setIdProof(clean(d.idProof()));
        visits.save(v);
        audit.notifyUser(resident, "New visitor request from " + v.getVisitorName() + " (" + v.getPurpose() + ")");
        return v;
    }

    private void validateRoleFields(RegisterRequest r, List<String> e) {
        switch (r.role()) {
            case GUEST -> { }
            case RESIDENT -> {
                need(e, "unitNumber", r.unitNumber());
                need(e, "governmentIdProof", r.governmentIdProof());
                need(e, "numberOfOccupants", r.numberOfOccupants());
                need(e, "emergencyContact", r.emergencyContact());
                need(e, "moveInDate", r.moveInDate());
                if (r.numberOfOccupants() != null && r.numberOfOccupants() < 1)
                    e.add("numberOfOccupants: must be at least 1");
                checkUnit(r, e);
            }
            case SECURITY_GUARD -> {
                need(e, "address", r.address());
                need(e, "employeeId", r.employeeId());
                need(e, "governmentIdProof", r.governmentIdProof());
                need(e, "assignedGateOrBlock", r.assignedGateOrBlock());
                need(e, "dutyShift", r.dutyShift());
                need(e, "joiningDate", r.joiningDate());
                need(e, "emergencyContact", r.emergencyContact());
            }
            case MAINTENANCE_STAFF -> {
                need(e, "address", r.address());
                need(e, "employeeId", r.employeeId());
                need(e, "department", r.department());
                need(e, "dutyShift", r.dutyShift());
                need(e, "joiningDate", r.joiningDate());
                need(e, "emergencyContact", r.emergencyContact());
            }
            case APARTMENT_MANAGER -> {
                need(e, "address", r.address());
                need(e, "employeeId", r.employeeId());
                need(e, "designation", r.designation());
                need(e, "assignedZone", r.assignedZone());
                need(e, "joiningDate", r.joiningDate());
                need(e, "emergencyContact", r.emergencyContact());
            }
            case ADMINISTRATOR -> {
                need(e, "address", r.address());
                need(e, "unitNumber", r.unitNumber());
                need(e, "governmentIdProof", r.governmentIdProof());
                need(e, "emergencyContact", r.emergencyContact());
                need(e, "employeeId", r.employeeId());
                need(e, "designation", r.designation());
                need(e, "appointmentDate", r.appointmentDate());
                need(e, "scopeOfAccess", r.scopeOfAccess());
                checkUnit(r, e);
            }
        }
    }

    private void checkUnit(RegisterRequest r, List<String> e) {
        String unit = clean(r.unitNumber());
        if (unit != null && !units.existsById(unit.toUpperCase()))
            e.add("unitNumber: not a recognized apartment unit, please enter a valid unit number");
    }

    private static void need(List<String> errors, String name, Object value) {
        if (value == null || (value instanceof String s && s.isBlank()))
            errors.add(name + ": is required for this role");
    }

    private static String clean(String s) { return s == null || s.isBlank() ? null : s.trim(); }

    // ---------------------------------------------------------------- US02 Login

    @Transactional(noRollbackFor = ApiException.class)
    public Map<String, Object> login(LoginRequest req) {
        String id = req.identifier().trim();
        User u = users.findByEmail(id.toLowerCase())
                .or(() -> users.findByPhone(id))
                .or(() -> users.findByEmployeeId(id))
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, INVALID));
        if (u.getStatus() == AccountStatus.DELETED) throw new ApiException(HttpStatus.UNAUTHORIZED, INVALID);

        LocalDateTime now = LocalDateTime.now();
        if (u.getLockUntil() != null && u.getLockUntil().isAfter(now))
            throw new ApiException(HttpStatus.LOCKED,
                    "Account temporarily locked due to repeated failed attempts. Try again later.");

        if (!encoder.matches(req.password(), u.getPasswordHash())) {
            u.setFailedAttempts(u.getFailedAttempts() + 1);
            if (u.getFailedAttempts() >= MAX_ATTEMPTS) {
                u.setLockUntil(now.plusMinutes(LOCK_MINUTES));
                u.setFailedAttempts(0);
                audit.notifyUser(u, "Your account has been temporarily locked after repeated failed logins.");
                if (u.getRole() == Role.ADMINISTRATOR)
                    audit.securityAlert("Administrator account locked after repeated failed logins: user " + u.getId());
            }
            users.save(u);
            throw new ApiException(HttpStatus.UNAUTHORIZED, INVALID);
        }

        u.setFailedAttempts(0);
        u.setLockUntil(null);
        users.save(u);

        if (u.getRole() == Role.GUEST)
            throw new ApiException(HttpStatus.FORBIDDEN,
                    "Guest accounts cannot log in until an Administrator assigns a role.");
        switch (u.getStatus()) {
            case PENDING -> throw new ApiException(HttpStatus.FORBIDDEN, "Your account is pending Administrator approval.");
            case REJECTED -> throw new ApiException(HttpStatus.FORBIDDEN, "Your registration was rejected.");
            case DEACTIVATED -> throw new ApiException(HttpStatus.FORBIDDEN, "Your account has been deactivated.");
            default -> { }
        }

        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        UserSession s = new UserSession();
        s.setToken(token);
        s.setUserId(u.getId());
        sessions.save(s);

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("token", token);
        out.put("tokenType", "Bearer");
        out.put("fullName", u.getFullName());
        out.put("role", u.getRole());
        out.put("idleTimeoutMinutes", IDLE_MINUTES);
        return out;
    }

    /** Called by the interceptor on every protected request. Role/status are re-read each time,
     *  so role changes, deactivation and deletion take effect immediately. */
    @Transactional(noRollbackFor = ApiException.class)
    public AuthContext authenticate(String token) {
        UserSession s = sessions.findByTokenAndActiveTrue(token)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED,
                        "Session invalid or expired. Please log in again."));
        LocalDateTime now = LocalDateTime.now();
        if (s.getLastAccessAt().plusMinutes(IDLE_MINUTES).isBefore(now)) {
            end(s, "TIMEOUT");
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Session timed out. Please log in again.");
        }
        User u = users.findById(s.getUserId()).orElse(null);
        if (u == null || u.getStatus() != AccountStatus.ACTIVE || u.getRole() == Role.GUEST) {
            end(s, "ACCOUNT_INACTIVE");
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Account is not active. Contact the Administrator.");
        }
        s.setLastAccessAt(now);
        sessions.save(s);
        return new AuthContext(u, s);
    }

    @Transactional
    public void changePassword(User user, UserSession current, ChangePasswordRequest r) {
        User u = users.findById(user.getId()).orElseThrow();
        if (!encoder.matches(r.currentPassword(), u.getPasswordHash()))
            throw new ApiException(HttpStatus.BAD_REQUEST, "Current password is incorrect");
        if (!PASSWORD_POLICY.matcher(r.newPassword()).matches())
            throw new ApiException(HttpStatus.BAD_REQUEST, PASSWORD_MSG);
        u.setPasswordHash(encoder.encode(r.newPassword()));
        users.save(u);
        sessions.findByUserIdAndActiveTrue(u.getId()).stream()
                .filter(s -> !s.getId().equals(current.getId()))
                .forEach(s -> end(s, "PASSWORD_CHANGED"));
        audit.log(u.getId(), "PASSWORD_CHANGE", u.getId(), null);
    }

    // ---------------------------------------------------------------- US03 Logout

    @Transactional
    public void logout(UserSession s) { end(s, "LOGOUT"); }

    @Transactional
    public void revokeAllSessions(Long userId, String reason) {
        sessions.findByUserIdAndActiveTrue(userId).forEach(s -> end(s, reason));
    }

    @Transactional(readOnly = true)
    public List<SessionResponse> listSessions(Long userId, Long currentSessionId) {
        return sessions.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(s -> new SessionResponse(s.getId(), s.getCreatedAt(), s.getLastAccessAt(), s.getEndedAt(),
                        s.isActive(), s.getEndedReason(), s.getId().equals(currentSessionId)))
                .toList();
    }

    private void end(UserSession s, String reason) {
        s.setActive(false);
        s.setEndedAt(LocalDateTime.now());
        s.setEndedReason(reason);
        sessions.save(s);   // token can never be reused after this
    }
}