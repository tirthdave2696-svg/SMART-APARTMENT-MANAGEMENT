package com.example.demo.service;

import com.example.demo.dto.ComplaintDtos.*;
import com.example.demo.exception.ApiException;
import com.example.demo.model.*;
import com.example.demo.repository.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ComplaintService {
    public static final Set<String> CATEGORIES = Set.of(
            "PLUMBING", "ELECTRICAL", "CARPENTRY", "CLEANING", "LIFT", "SECURITY", "PEST_CONTROL", "OTHER");

    private final ComplaintRepository complaints;
    private final UserRepository users;
    private final AuditService audit;

    public ComplaintService(ComplaintRepository complaints, UserRepository users, AuditService audit) {
        this.complaints = complaints; this.users = users; this.audit = audit;
    }

    private Complaint find(Long id) {
        return complaints.findById(id).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Complaint not found"));
    }

    private static boolean has(String s) { return s != null && !s.isBlank(); }

    private static String category(String c) {
        String norm = c.trim().toUpperCase().replace(' ', '_');
        if (!CATEGORIES.contains(norm))
            throw new ApiException(HttpStatus.BAD_REQUEST, "category: must be one of " + new TreeSet<>(CATEGORIES));
        return norm;
    }

    private static void access(User actor, Complaint c) {
        if (actor.getRole() == Role.RESIDENT && !actor.getId().equals(c.getResidentId()))
            throw new ApiException(HttpStatus.FORBIDDEN, "You can only access your own complaints");
        if (actor.getRole() == Role.MAINTENANCE_STAFF && !actor.getId().equals(c.getAssignedToId()))
            throw new ApiException(HttpStatus.FORBIDDEN, "You can only access complaints assigned to you");
    }

    private static Object shape(User actor, Complaint c) {
        return actor.getRole() == Role.MAINTENANCE_STAFF ? ComplaintStaffView.from(c) : ComplaintResponse.from(c);
    }

    // ------------------------------------------------------------ T13 Raise

    @Transactional
    public ComplaintResponse raise(User actor, RaiseComplaintRequest d) {
        User resident;
        if (actor.getRole() == Role.ADMINISTRATOR) {
            if (d.residentId() == null)
                throw new ApiException(HttpStatus.BAD_REQUEST, "residentId: required when raising a complaint on a resident's behalf");
            resident = users.findById(d.residentId())
                    .filter(u -> u.getRole() == Role.RESIDENT && u.getStatus() == AccountStatus.ACTIVE)
                    .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Active resident not found"));
        } else {
            resident = users.findById(actor.getId()).orElseThrow();
        }
        if (!has(resident.getUnitNumber()))
            throw new ApiException(HttpStatus.BAD_REQUEST, "The resident is not linked to a unit");
        if (has(d.unitNumber()) && !d.unitNumber().trim().equalsIgnoreCase(resident.getUnitNumber()))
            throw new ApiException(HttpStatus.BAD_REQUEST, "unitNumber: does not match the resident's registered unit");
        if (d.complaintDate().isAfter(LocalDate.now()))
            throw new ApiException(HttpStatus.BAD_REQUEST, "complaintDate: cannot be in the future");

        Complaint c = new Complaint();
        c.setResidentId(resident.getId());
        c.setResidentName(resident.getFullName());
        c.setUnitNumber(resident.getUnitNumber());
        c.setDescription(d.description().trim());
        c.setCategory(category(d.category()));
        c.setComplaintDate(d.complaintDate());
        c.setStatus("OPEN");
        complaints.save(c);
        audit.log(actor.getId(), "COMPLAINT_RAISE", resident.getId(), "complaint " + c.getId() + " " + c.getCategory());
        return ComplaintResponse.from(c);
    }

    @Transactional(readOnly = true)
    public List<Object> list(User actor, String status, String category) {
        List<Complaint> base = switch (actor.getRole()) {
            case RESIDENT -> complaints.findByResidentIdOrderByCreatedAtDesc(actor.getId());
            case MAINTENANCE_STAFF -> complaints.findByAssignedToIdOrderByCreatedAtDesc(actor.getId());
            default -> complaints.findAllByOrderByCreatedAtDesc();
        };
        return base.stream()
                .filter(c -> !has(status) || c.getStatus().equalsIgnoreCase(status.trim()))
                .filter(c -> !has(category) || c.getCategory().equalsIgnoreCase(category.trim().replace(' ', '_')))
                .map(c -> shape(actor, c)).toList();
    }

    @Transactional(readOnly = true)
    public Object get(User actor, Long id) {
        Complaint c = find(id);
        access(actor, c);
        return shape(actor, c);
    }

    @Transactional
    public ComplaintResponse edit(User actor, Long id, EditComplaintRequest d) {
        Complaint c = find(id);
        access(actor, c);
        if (!"OPEN".equals(c.getStatus()))
            throw new ApiException(HttpStatus.CONFLICT, "A complaint can only be edited before staff assignment");
        if (!has(d.description()) && !has(d.category()))
            throw new ApiException(HttpStatus.BAD_REQUEST, "No changes supplied");
        if (has(d.description())) c.setDescription(d.description().trim());
        if (has(d.category())) c.setCategory(category(d.category()));
        c.setUpdatedAt(LocalDateTime.now());
        complaints.save(c);
        audit.log(actor.getId(), "COMPLAINT_EDIT", c.getResidentId(), "complaint " + id);
        return ComplaintResponse.from(c);
    }

    // ------------------------------------------------------------ Assignment and status (supporting)

    @Transactional
    public ComplaintResponse assign(User admin, Long id, Long staffId) {
        Complaint c = find(id);
        if (!"OPEN".equals(c.getStatus()) && !"ASSIGNED".equals(c.getStatus()))
            throw new ApiException(HttpStatus.CONFLICT, "Only OPEN or ASSIGNED complaints can be (re)assigned");
        User staff = users.findById(staffId)
                .filter(u -> u.getRole() == Role.MAINTENANCE_STAFF && u.getStatus() == AccountStatus.ACTIVE)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Active maintenance staff member not found"));
        c.setAssignedToId(staff.getId());
        c.setAssignedToName(staff.getFullName());
        c.setStatus("ASSIGNED");
        c.setUpdatedAt(LocalDateTime.now());
        complaints.save(c);
        audit.log(admin.getId(), "COMPLAINT_ASSIGN", c.getResidentId(), "complaint " + id + " -> staff " + staffId);
        audit.notifyUser(staff, "Complaint #" + id + " (" + c.getCategory() + ", unit " + c.getUnitNumber() + ") was assigned to you.");
        return ComplaintResponse.from(c);
    }

    @Transactional
    public Object updateStatus(User actor, Long id, ComplaintStatusRequest d) {
        Complaint c = find(id);
        access(actor, c);
        String target = d.status().trim().toUpperCase();
        boolean admin = actor.getRole() == Role.ADMINISTRATOR;
        if (!target.equals("IN_PROGRESS") && !target.equals("RESOLVED") && !(admin && target.equals("CLOSED")))
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "status must be IN_PROGRESS or RESOLVED" + (admin ? " or CLOSED" : ""));
        if ("CLOSED".equals(c.getStatus()))
            throw new ApiException(HttpStatus.CONFLICT, "This complaint is already closed");
        if (!target.equals("CLOSED") && !"ASSIGNED".equals(c.getStatus()) && !"IN_PROGRESS".equals(c.getStatus()))
            throw new ApiException(HttpStatus.CONFLICT, "The complaint must be assigned (and not yet resolved) before its progress is updated");
        c.setStatus(target);
        if (has(d.note())) c.setResolutionNote(d.note().trim());
        c.setUpdatedAt(LocalDateTime.now());
        complaints.save(c);
        audit.log(actor.getId(), "COMPLAINT_STATUS", c.getResidentId(), "complaint " + id + " -> " + target);
        users.findById(c.getResidentId()).ifPresent(r ->
                audit.notifyUser(r, "Your complaint #" + id + " is now " + target));
        return shape(actor, c);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> summary() {
        List<Complaint> all = complaints.findAll();
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("total", all.size());
        out.put("byCategory", all.stream().collect(Collectors.groupingBy(Complaint::getCategory, TreeMap::new, Collectors.counting())));
        out.put("byStatus", all.stream().collect(Collectors.groupingBy(Complaint::getStatus, TreeMap::new, Collectors.counting())));
        return out;
    }
}