package com.example.demo.service;

import com.example.demo.model.*;
import com.example.demo.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class DashboardService {
    private final UserRepository users;
    private final VisitRequestRepository visits;
    private final ComplaintRepository complaints;
    private final NoticeRepository notices;
    private final ResidentService residents;

    public DashboardService(UserRepository users, VisitRequestRepository visits, ComplaintRepository complaints,
                            NoticeRepository notices, ResidentService residents) {
        this.users = users; this.visits = visits; this.complaints = complaints;
        this.notices = notices; this.residents = residents;
    }

    // ---- T21 Dashboard (content depends on role)

    @Transactional(readOnly = true)
    public Map<String, Object> dashboard(User actor) {
        Role role = actor.getRole();
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("user", actor.getFullName());
        m.put("role", role);
        m.put("activeNotices", notices.findAll().stream().filter(n -> NoticeService.isActiveFor(n, role)).count());

        switch (role) {
            case ADMINISTRATOR, APARTMENT_MANAGER -> {
                List<User> all = users.findAll();
                m.put("activeUsersByRole", all.stream().filter(u -> u.getStatus() == AccountStatus.ACTIVE)
                        .collect(Collectors.groupingBy(u -> u.getRole().name(), TreeMap::new, Collectors.counting())));
                m.put("pendingRegistrations", all.stream().filter(u -> u.getStatus() == AccountStatus.PENDING).count());
                m.put("activeResidents", all.stream()
                        .filter(u -> u.getRole() == Role.RESIDENT && u.getStatus() == AccountStatus.ACTIVE).count());
                m.put("vacantUnits", residents.vacantUnits().size());
                m.put("visitors", Stats.visitors(visits.findAllByOrderByCreatedAtDesc()));
                m.put("complaints", Stats.complaints(complaints.findAll()));
            }
            case RESIDENT -> {
                m.put("unit", actor.getUnitNumber());
                m.put("visitors", Stats.visitors(visits.findByResidentIdOrderByCreatedAtDesc(actor.getId())));
                m.put("complaints", Stats.complaints(complaints.findByResidentIdOrderByCreatedAtDesc(actor.getId())));
            }
            case SECURITY_GUARD -> {
                m.put("visitors", Stats.visitors(visits.findAllByOrderByCreatedAtDesc()));
                m.put("preRegisteredWaiting", visits.findAll().stream()
                        .filter(v -> VisitStatus.PRE_REGISTERED.equals(v.getStatus())).count());
            }
            case MAINTENANCE_STAFF -> m.put("complaints",
                    Stats.complaints(complaints.findByAssignedToIdOrderByCreatedAtDesc(actor.getId())));
            default -> { }
        }
        return m;
    }

    // ---- T22 Visitor summary

    @Transactional(readOnly = true)
    public Map<String, Object> visitorSummary(User actor) {
        List<VisitRequest> list = actor.getRole() == Role.RESIDENT
                ? visits.findByResidentIdOrderByCreatedAtDesc(actor.getId())
                : visits.findAllByOrderByCreatedAtDesc();
        return Stats.visitors(list);
    }

    // ---- T23 Complaint summary

    @Transactional(readOnly = true)
    public Map<String, Object> complaintSummary(User actor) {
        List<Complaint> list = switch (actor.getRole()) {
            case RESIDENT -> complaints.findByResidentIdOrderByCreatedAtDesc(actor.getId());
            case MAINTENANCE_STAFF -> complaints.findByAssignedToIdOrderByCreatedAtDesc(actor.getId());
            default -> complaints.findAllByOrderByCreatedAtDesc();
        };
        return Stats.complaints(list);
    }
}