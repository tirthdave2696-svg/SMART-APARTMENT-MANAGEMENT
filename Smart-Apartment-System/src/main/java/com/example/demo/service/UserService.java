package com.example.demo.service;

import com.example.demo.dto.Dtos.*;
import com.example.demo.exception.ApiException;
import com.example.demo.model.*;
import com.example.demo.repository.UserRepository;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
public class UserService {
    private final UserRepository users;
    private final AuthService authService;
    private final AuditService audit;

    public UserService(UserRepository users, AuthService authService, AuditService audit) {
        this.users = users; this.authService = authService; this.audit = audit;
    }

    private User find(Long id) {
        return users.findById(id).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found"));
    }

    private void notDeleted(User u) {
        if (u.getStatus() == AccountStatus.DELETED)
            throw new ApiException(HttpStatus.CONFLICT, "This account has been deleted");
    }

    private void notSelf(Long id, User admin, String what) {
        if (id.equals(admin.getId()))
            throw new ApiException(HttpStatus.BAD_REQUEST, "You cannot " + what + " your own account");
    }

    // ------------------------------------------------------------ Own profile

    @Transactional
    public UserResponse updateProfile(User me, UpdateProfileRequest d) {
        User u = find(me.getId());
        if (d.fullName() != null && !d.fullName().isBlank()) u.setFullName(d.fullName().trim());
        if (d.phone() != null && !d.phone().isBlank()) {
            String p = d.phone().trim();
            users.findByPhone(p).ifPresent(other -> {
                if (!other.getId().equals(u.getId()))
                    throw new ApiException(HttpStatus.BAD_REQUEST, "phone: already registered");
            });
            u.setPhone(p);
        }
        if (d.address() != null && !d.address().isBlank()) u.setAddress(d.address().trim());
        if (d.emergencyContact() != null && !d.emergencyContact().isBlank()) u.setEmergencyContact(d.emergencyContact().trim());
        users.save(u);
        return UserResponse.from(u);
    }

    @Transactional
    public void requestCorrection(User me, String message) {
        audit.log(me.getId(), "CORRECTION_REQUEST", me.getId(), message);
    }

    // ------------------------------------------------------------ US04 Admin operations

    public List<UserResponse> list(AccountStatus status, Role role) {
        return users.findAll(Sort.by(Sort.Direction.DESC, "createdAt")).stream()
                .filter(u -> status == null || u.getStatus() == status)
                .filter(u -> role == null || u.getRole() == role)
                .map(UserResponse::from).toList();
    }

    @Transactional
    public UserResponse approve(Long id, User admin) {
        User u = find(id);
        if (u.getStatus() != AccountStatus.PENDING)
            throw new ApiException(HttpStatus.CONFLICT, "Only pending registrations can be approved");
        u.setStatus(AccountStatus.ACTIVE);
        users.save(u);
        audit.log(admin.getId(), "APPROVE", id, null);
        audit.notifyUser(u, "Your registration has been approved. You can now log in.");
        return UserResponse.from(u);
    }

    @Transactional
    public UserResponse reject(Long id, User admin, String reason) {
        User u = find(id);
        if (u.getStatus() != AccountStatus.PENDING)
            throw new ApiException(HttpStatus.CONFLICT, "Only pending registrations can be rejected");
        u.setStatus(AccountStatus.REJECTED);
        users.save(u);
        audit.log(admin.getId(), "REJECT", id, reason);
        audit.notifyUser(u, "Your registration was rejected." + (reason == null ? "" : " Reason: " + reason));
        return UserResponse.from(u);
    }

    @Transactional
    public UserResponse changeRole(Long id, User admin, RoleChangeRequest d) {
        notSelf(id, admin, "change the role of");
        User u = find(id);
        notDeleted(u);
        Role old = u.getRole();
        u.setRole(d.role());
        users.save(u);   // active sessions pick up the new role on their next request
        audit.log(admin.getId(), "ROLE_CHANGE", id, old + " -> " + d.role() + ": " + d.reason());
        audit.notifyUser(u, "Your role was changed to " + d.role());
        return UserResponse.from(u);
    }

    @Transactional
    public UserResponse changeStatus(Long id, User admin, StatusChangeRequest d) {
        if (d.status() != AccountStatus.ACTIVE && d.status() != AccountStatus.DEACTIVATED)
            throw new ApiException(HttpStatus.BAD_REQUEST, "status must be ACTIVE or DEACTIVATED");
        notSelf(id, admin, "change the status of");
        User u = find(id);
        notDeleted(u);
        u.setStatus(d.status());
        users.save(u);
        if (d.status() == AccountStatus.DEACTIVATED) authService.revokeAllSessions(id, "DEACTIVATED");
        audit.log(admin.getId(), "STATUS_CHANGE", id, d.status() + ": " + d.reason());
        audit.notifyUser(u, "Your account status is now " + d.status());
        return UserResponse.from(u);
    }

    /** Soft delete: login access is removed, historical records stay for audit/reporting. */
    @Transactional
    public void delete(Long id, User admin, String reason) {
        notSelf(id, admin, "delete");
        User u = find(id);
        notDeleted(u);
        u.setStatus(AccountStatus.DELETED);
        users.save(u);
        authService.revokeAllSessions(id, "DELETED");
        audit.log(admin.getId(), "DELETE", id, reason);
    }

    @Transactional
    public void forceLogout(Long id, User admin) {
        find(id);
        authService.revokeAllSessions(id, "FORCED_LOGOUT");
        audit.log(admin.getId(), "FORCE_LOGOUT", id, null);
    }
}