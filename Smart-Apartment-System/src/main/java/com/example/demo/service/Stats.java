package com.example.demo.service;

import com.example.demo.model.Complaint;
import com.example.demo.model.VisitRequest;
import com.example.demo.model.VisitStatus;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

public final class Stats {
    private Stats() {}

    public static Map<String, Object> visitors(List<VisitRequest> list) {
        LocalDate today = LocalDate.now();
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("total", list.size());
        m.put("registeredToday", list.stream()
                .filter(v -> v.getCreatedAt() != null && v.getCreatedAt().toLocalDate().equals(today)).count());
        m.put("insideNow", list.stream().filter(v -> VisitStatus.INSIDE.equals(v.getStatus())).count());
        m.put("pendingApproval", list.stream().filter(v -> VisitStatus.PENDING.equals(v.getStatus())).count());
        OptionalDouble avg = list.stream().filter(v -> v.getDurationMinutes() != null)
                .mapToLong(VisitRequest::getDurationMinutes).average();
        m.put("averageVisitMinutes", avg.isPresent() ? Math.round(avg.getAsDouble() * 10) / 10.0 : 0.0);
        m.put("byStatus", list.stream()
                .collect(Collectors.groupingBy(VisitRequest::getStatus, TreeMap::new, Collectors.counting())));
        return m;
    }

    public static Map<String, Object> complaints(List<Complaint> list) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("total", list.size());
        m.put("open", list.stream().filter(c -> c.getStatus().equals("OPEN") || c.getStatus().equals("ASSIGNED")
                || c.getStatus().equals("IN_PROGRESS")).count());
        m.put("resolved", list.stream().filter(c -> c.getStatus().equals("RESOLVED")).count());
        m.put("closed", list.stream().filter(c -> c.getStatus().equals("CLOSED")).count());
        m.put("byStatus", list.stream()
                .collect(Collectors.groupingBy(Complaint::getStatus, TreeMap::new, Collectors.counting())));
        m.put("byCategory", list.stream()
                .collect(Collectors.groupingBy(Complaint::getCategory, TreeMap::new, Collectors.counting())));
        return m;
    }

    public static String csv(Object o) {
        if (o == null) return "\"\"";
        return "\"" + String.valueOf(o).replace("\"", "\"\"") + "\"";
    }
}