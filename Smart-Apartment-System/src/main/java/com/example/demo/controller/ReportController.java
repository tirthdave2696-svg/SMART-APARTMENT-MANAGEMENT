package com.example.demo.controller;

import com.example.demo.config.AuthInterceptor;
import com.example.demo.config.RequireRoles;
import com.example.demo.model.Role;
import com.example.demo.model.User;
import com.example.demo.service.ReportService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/reports")
@RequireRoles({Role.APARTMENT_MANAGER, Role.ADMINISTRATOR})
public class ReportController {
    private final ReportService service;

    public ReportController(ReportService service) { this.service = service; }

    @GetMapping("/activity")
    public Map<String, Object> activity(@RequestAttribute(AuthInterceptor.USER_ATTR) User user,
                                        @RequestParam(name = "from", required = false) String from,
                                        @RequestParam(name = "to", required = false) String to) {
        return service.activity(user, from, to);
    }

    @GetMapping("/visitors/export")
    public ResponseEntity<String> visitorsCsv(@RequestParam(name = "from", required = false) String from,
                                              @RequestParam(name = "to", required = false) String to) {
        return ResponseEntity.ok().header("Content-Type", "text/csv")
                .header("Content-Disposition", "attachment; filename=visitor-report.csv")
                .body(service.visitorsCsv(from, to));
    }

    @GetMapping("/complaints/export")
    public ResponseEntity<String> complaintsCsv(@RequestParam(name = "from", required = false) String from,
                                                @RequestParam(name = "to", required = false) String to) {
        return ResponseEntity.ok().header("Content-Type", "text/csv")
                .header("Content-Disposition", "attachment; filename=complaint-report.csv")
                .body(service.complaintsCsv(from, to));
    }
}