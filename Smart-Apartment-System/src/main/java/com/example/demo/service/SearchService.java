package com.example.demo.service;

import com.example.demo.dto.ComplaintDtos.*;
import com.example.demo.dto.VisitDtos.VisitResponse;
import com.example.demo.model.*;
import com.example.demo.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.util.List;

@Service
public class SearchService {
    private final VisitRequestRepository visits;
    private final ComplaintRepository complaints;

    public SearchService(VisitRequestRepository visits, ComplaintRepository complaints) {
        this.visits = visits; this.complaints = complaints;
    }

    private static boolean has(String s) { return s != null && !s.isBlank(); }

    private static boolean hit(String needle, String... fields) {
        for (String f : fields) if (f != null && f.toLowerCase().contains(needle)) return true;
        return false;
    }

    /** T25 search and T26 filter in one call: every parameter is optional and they combine. */
    @Transactional(readOnly = true)
    public List<VisitResponse> visitors(User actor, String q, String status, String unit,
                                        String from, String to, Boolean preRegistered) {
        LocalDate f = DateUtil.parse("from", from);
        LocalDate t = DateUtil.parse("to", to);
        DateUtil.order(f, t);
        String needle = has(q) ? q.trim().toLowerCase() : "";
        List<VisitRequest> base = actor.getRole() == Role.RESIDENT
                ? visits.findByResidentIdOrderByCreatedAtDesc(actor.getId())
                : visits.findAllByOrderByCreatedAtDesc();
        return base.stream()
                .filter(v -> needle.isEmpty() || hit(needle, v.getVisitorName(), v.getPhone(), v.getPurpose(),
                        v.getVehicleNumber(), v.getResidentName(), v.getUnitNumber()))
                .filter(v -> !has(status) || v.getStatus().equalsIgnoreCase(status.trim()))
                .filter(v -> !has(unit) || unit.trim().equalsIgnoreCase(v.getUnitNumber()))
                .filter(v -> preRegistered == null || v.isPreRegistered() == preRegistered)
                .filter(v -> (f == null && t == null)
                        || (v.getCreatedAt() != null && DateUtil.within(v.getCreatedAt().toLocalDate(), f, t)))
                .map(VisitResponse::from).toList();
    }

    /** T27 search and T28 filter in one call. */
    @Transactional(readOnly = true)
    public List<Object> complaints(User actor, String q, String status, String category, String unit,
                                   String from, String to, Long assignedTo) {
        LocalDate f = DateUtil.parse("from", from);
        LocalDate t = DateUtil.parse("to", to);
        DateUtil.order(f, t);
        String needle = has(q) ? q.trim().toLowerCase() : "";
        boolean staff = actor.getRole() == Role.MAINTENANCE_STAFF;
        List<Complaint> base = switch (actor.getRole()) {
            case RESIDENT -> complaints.findByResidentIdOrderByCreatedAtDesc(actor.getId());
            case MAINTENANCE_STAFF -> complaints.findByAssignedToIdOrderByCreatedAtDesc(actor.getId());
            default -> complaints.findAllByOrderByCreatedAtDesc();
        };
        return base.stream()
                .filter(c -> needle.isEmpty() || hit(needle, c.getDescription(), c.getCategory(), c.getUnitNumber(),
                        c.getStatus(), staff ? null : c.getResidentName()))
                .filter(c -> !has(status) || c.getStatus().equalsIgnoreCase(status.trim()))
                .filter(c -> !has(category) || c.getCategory().equalsIgnoreCase(category.trim().replace(' ', '_')))
                .filter(c -> !has(unit) || unit.trim().equalsIgnoreCase(c.getUnitNumber()))
                .filter(c -> assignedTo == null || assignedTo.equals(c.getAssignedToId()))
                .filter(c -> DateUtil.within(c.getComplaintDate(), f, t))
                .map(c -> staff ? (Object) ComplaintStaffView.from(c) : (Object) ComplaintResponse.from(c))
                .toList();
    }
}