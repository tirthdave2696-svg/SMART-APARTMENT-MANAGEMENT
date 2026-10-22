package com.example.demo.service;

import com.example.demo.dto.NoticeDtos.*;
import com.example.demo.exception.ApiException;
import com.example.demo.model.*;
import com.example.demo.repository.NoticeRepository;
import com.example.demo.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class NoticeService {
    public static final Set<String> TYPES = new TreeSet<>(List.of("GENERAL", "EMERGENCY", "MAINTENANCE", "EVENT"));
    private static final Set<Role> AUDIENCE_ROLES =
            EnumSet.of(Role.RESIDENT, Role.SECURITY_GUARD, Role.MAINTENANCE_STAFF, Role.APARTMENT_MANAGER);

    private final NoticeRepository notices;
    private final UserRepository users;
    private final AuditService audit;

    public NoticeService(NoticeRepository notices, UserRepository users, AuditService audit) {
        this.notices = notices; this.users = users; this.audit = audit;
    }

    // ---- visibility rules (also used by the dashboard)

    public static boolean isActive(Notice n) {
        return n.getExpiresOn() == null || !n.getExpiresOn().isBefore(LocalDate.now());
    }

    public static boolean audienceMatches(Notice n, Role role) {
        return "ALL".equals(n.getAudience()) || n.getAudience().equals(role.name());
    }

    /** Active notices a role is allowed to see. Administrators see every audience. */
    public static boolean isActiveFor(Notice n, Role role) {
        return isActive(n) && (role == Role.ADMINISTRATOR || audienceMatches(n, role));
    }

    private static boolean visibleTo(Notice n, Role role) {
        return role == Role.ADMINISTRATOR || isActiveFor(n, role);   // admin also sees expired ones
    }

    // ---- validation helpers

    private static boolean has(String s) { return s != null && !s.isBlank(); }

    private static String normType(String t) {
        if (!has(t)) return "GENERAL";
        String v = t.trim().toUpperCase();
        if (!TYPES.contains(v)) throw new ApiException(HttpStatus.BAD_REQUEST, "type: must be one of " + TYPES);
        return v;
    }

    private static String normAudience(String a) {
        if (!has(a)) return "ALL";
        String v = a.trim().toUpperCase();
        if (v.equals("ALL")) return v;
        try {
            Role r = Role.valueOf(v);
            if (AUDIENCE_ROLES.contains(r)) return v;
        } catch (IllegalArgumentException ignored) { }
        throw new ApiException(HttpStatus.BAD_REQUEST,
                "audience: must be ALL or one of " + AUDIENCE_ROLES);
    }

    private static LocalDate checkExpiry(LocalDate d) {
        if (d != null && d.isBefore(LocalDate.now()))
            throw new ApiException(HttpStatus.BAD_REQUEST, "expiresOn: cannot be in the past");
        return d;
    }

    private Notice find(Long id) {
        return notices.findById(id).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Notice not found"));
    }

    // ---- T17 Create

    @Transactional
    public Map<String, Object> create(User admin, NoticeRequest d) {
        Notice n = new Notice();
        n.setTitle(d.title().trim());
        n.setMessage(d.message().trim());
        n.setNoticeType(normType(d.type()));
        n.setAudience(normAudience(d.audience()));
        n.setExpiresOn(checkExpiry(d.expiresOn()));
        n.setCreatedById(admin.getId());
        n.setCreatedByName(admin.getFullName());
        notices.save(n);
        audit.log(admin.getId(), "NOTICE_CREATE", null, "notice " + n.getId() + ": " + n.getTitle());

        int notified = 0;
        if ("EMERGENCY".equals(n.getNoticeType())) {   // emergency notices are pushed to everyone in the audience
            for (User u : users.findAll()) {
                if (u.getStatus() == AccountStatus.ACTIVE && u.getRole() != Role.GUEST && audienceMatches(n, u.getRole())) {
                    audit.notifyUser(u, "EMERGENCY NOTICE: " + n.getTitle() + " - " + n.getMessage());
                    notified++;
                }
            }
        }
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("message", "Notice published");
        out.put("notice", NoticeResponse.from(n));
        if ("EMERGENCY".equals(n.getNoticeType())) out.put("usersNotified", notified);
        return out;
    }

    // ---- T18 View

    @Transactional(readOnly = true)
    public List<NoticeResponse> list(User actor, String type) {
        return notices.findAllByOrderByCreatedAtDesc().stream()
                .filter(n -> visibleTo(n, actor.getRole()))
                .filter(n -> !has(type) || n.getNoticeType().equalsIgnoreCase(type.trim()))
                .map(NoticeResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public NoticeResponse get(User actor, Long id) {
        Notice n = find(id);
        if (!visibleTo(n, actor.getRole()))
            throw new ApiException(HttpStatus.FORBIDDEN, "You are not authorised to view this notice");
        return NoticeResponse.from(n);
    }

    // ---- T19 Edit

    @Transactional
    public NoticeResponse edit(User admin, Long id, EditNoticeRequest d) {
        Notice n = find(id);
        if (!has(d.title()) && !has(d.message()) && !has(d.type()) && !has(d.audience()) && d.expiresOn() == null)
            throw new ApiException(HttpStatus.BAD_REQUEST, "No changes supplied");
        if (has(d.title())) n.setTitle(d.title().trim());
        if (has(d.message())) n.setMessage(d.message().trim());
        if (has(d.type())) n.setNoticeType(normType(d.type()));
        if (has(d.audience())) n.setAudience(normAudience(d.audience()));
        if (d.expiresOn() != null) n.setExpiresOn(checkExpiry(d.expiresOn()));
        n.setUpdatedAt(LocalDateTime.now());
        notices.save(n);
        audit.log(admin.getId(), "NOTICE_EDIT", null, "notice " + id);
        return NoticeResponse.from(n);
    }

    // ---- T20 Delete

    @Transactional
    public void delete(User admin, Long id) {
        Notice n = find(id);
        notices.delete(n);
        audit.log(admin.getId(), "NOTICE_DELETE", null, "notice " + id + ": " + n.getTitle());
    }
}