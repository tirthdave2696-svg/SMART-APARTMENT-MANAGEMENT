package com.example.demo.controller;

import com.example.demo.config.AuthInterceptor;
import com.example.demo.config.RequireRoles;
import com.example.demo.model.Role;
import com.example.demo.model.User;
import com.example.demo.service.DashboardService;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {
    private final DashboardService service;

    public DashboardController(DashboardService service) { this.service = service; }

    @RequireRoles({Role.RESIDENT, Role.SECURITY_GUARD, Role.MAINTENANCE_STAFF, Role.APARTMENT_MANAGER, Role.ADMINISTRATOR})
    @GetMapping
    public Map<String, Object> dashboard(@RequestAttribute(AuthInterceptor.USER_ATTR) User user) {
        return service.dashboard(user);
    }

    @RequireRoles({Role.RESIDENT, Role.SECURITY_GUARD, Role.APARTMENT_MANAGER, Role.ADMINISTRATOR})
    @GetMapping("/visitors")
    public Map<String, Object> visitors(@RequestAttribute(AuthInterceptor.USER_ATTR) User user) {
        return service.visitorSummary(user);
    }

    @RequireRoles({Role.RESIDENT, Role.MAINTENANCE_STAFF, Role.APARTMENT_MANAGER, Role.ADMINISTRATOR})
    @GetMapping("/complaints")
    public Map<String, Object> complaints(@RequestAttribute(AuthInterceptor.USER_ATTR) User user) {
        return service.complaintSummary(user);
    }
}