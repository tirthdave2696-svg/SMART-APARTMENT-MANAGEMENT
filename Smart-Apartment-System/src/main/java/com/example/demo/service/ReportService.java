package com.example.demo.service;

import com.example.demo.exception.ApiException;
import com.example.demo.model.*;
import com.example.demo.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ReportService {
    private final VisitRequestRepository visits;
    private final ComplaintRepository complaints;
    private final UserRepository users;
    private final ResidentService residents;

    public ReportService(VisitRequestRepository visits, ComplaintRepository complaints,
                         UserRepository users, ResidentService residents) {
        this.visits = visits; this.complaints = complaints; this.users = users; this.residents = residents;
    }

    /** Default period is the last 30 days. */
    private static LocalDate[] range(String from, String to) {
        LocalDate t = DateUtil.parse("to", to);
        LocalDate f = DateUtil.parse("from", from);
        if (t == null) t = LocalDate.now();
        if (f == null) f = t.minusDays(30);
        DateUtil.order(f, t);
        return new LocalDate[]{f, t};
    }

    private List<VisitRequest> visitsIn(LocalDate f, LocalDate t) {
        return visits.findAllByOrderByCreatedAtDesc().stream()
                .filter(v -> v.getCreatedAt() != null && DateUtil.within(v.getCreatedAt().toLocalDate(), f, t)).toList();
    }

    private List<Complaint> complaintsIn(LocalDate f, LocalDate t) {
        return complaints.findAllByOrderByCreatedAtDesc().stream()
                .filter(c -> c.getComplaintDate() != null && DateUtil.within(c.getComplaintDate(), f, t)).toList();
    }

    @Transactional(readOnly = true)
    public Map<String, Object> activity(User actor, String from, String to) {
        LocalDate[] r = range(from, to);
        LocalDate f = r[0], t = r[1];
        List<VisitRequest> vs = visitsIn(f, t);
        List<Complaint> cs = complaintsIn(f, t);

        Map<String, Object> visitorSection = Stats.visitors(vs);
        visitorSection.put("perDay", vs.stream().collect(Collectors.groupingBy(
                v -> v.getCreatedAt().toLocalDate().toString(), TreeMap::new, Collectors.counting())));

        Map<String, Object> residentSection = new LinkedHashMap<>();
        residentSection.put("activeResidents", users.findAll().stream()
                .filter(u -> u.getRole() == Role.RESIDENT && u.getStatus() == AccountStatus.ACTIVE).count());
        residentSection.put("vacantUnits", residents.vacantUnits().size());

        Map<String, Object> period = new LinkedHashMap<>();
        period.put("from", f.toString());
        period.put("to", t.toString());

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("report", "Apartment activity report");
        out.put("generatedAt", LocalDateTime.now().toString());
        out.put("generatedBy", actor.getFullName());
        out.put("period", period);
        out.put("visitors", visitorSection);
        out.put("complaints", Stats.complaints(cs));
        out.put("residents", residentSection);
        return out;
    }

    @Transactional(readOnly = true)
    public String visitorsCsv(String from, String to) {
        LocalDate[] r = range(from, to);
        StringBuilder sb = new StringBuilder(
                "ID,Visitor,Phone,Purpose,Unit,Resident,Status,Registered,Entry,Exit,DurationMinutes\n");
        for (VisitRequest v : visitsIn(r[0], r[1])) {
            sb.append(Stats.csv(v.getId())).append(',').append(Stats.csv(v.getVisitorName())).append(',')
              .append(Stats.csv(v.getPhone())).append(',').append(Stats.csv(v.getPurpose())).append(',')
              .append(Stats.csv(v.getUnitNumber())).append(',').append(Stats.csv(v.getResidentName())).append(',')
              .append(Stats.csv(v.getStatus())).append(',').append(Stats.csv(v.getCreatedAt())).append(',')
              .append(Stats.csv(v.getEntryTime())).append(',').append(Stats.csv(v.getExitTime())).append(',')
              .append(Stats.csv(v.getDurationMinutes())).append('\n');
        }
        return sb.toString();
    }

    @Transactional(readOnly = true)
    public String complaintsCsv(String from, String to) {
        LocalDate[] r = range(from, to);
        StringBuilder sb = new StringBuilder("ID,Unit,Resident,Category,Description,Date,Status,AssignedTo,Updated\n");
        for (Complaint c : complaintsIn(r[0], r[1])) {
            sb.append(Stats.csv(c.getId())).append(',').append(Stats.csv(c.getUnitNumber())).append(',')
              .append(Stats.csv(c.getResidentName())).append(',').append(Stats.csv(c.getCategory())).append(',')
              .append(Stats.csv(c.getDescription())).append(',').append(Stats.csv(c.getComplaintDate())).append(',')
              .append(Stats.csv(c.getStatus())).append(',').append(Stats.csv(c.getAssignedToName())).append(',')
              .append(Stats.csv(c.getUpdatedAt())).append('\n');
        }
        return sb.toString();
    }
}