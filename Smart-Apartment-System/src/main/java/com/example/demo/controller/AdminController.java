package com.example.demo.controller;

import com.example.demo.config.AuthInterceptor;
import com.example.demo.config.RequireRoles;
import com.example.demo.dto.Dtos.*;
import com.example.demo.model.*;
import com.example.demo.service.AuditService;
import com.example.demo.service.UserService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@RequireRoles(Role.ADMINISTRATOR)
public class AdminController {
    private final UserService userService;
    private final AuditService audit;

    public AdminController(UserService userService, AuditService audit) {
        this.userService = userService; this.audit = audit;
    }

    // Administrator: full list. Apartment Manager: same list, read-only (no write endpoints allowed to them).
    @RequireRoles({Role.ADMINISTRATOR, Role.APARTMENT_MANAGER})
    @GetMapping("/users")
    public List<UserResponse> list(@RequestParam(name = "status", required = false) AccountStatus status,
                                   @RequestParam(name = "role", required = false) Role role) {
        return userService.list(status, role);
    }

    @PutMapping("/users/{id}/approve")
    public UserResponse approve(@PathVariable("id") Long id,
                                @RequestAttribute(AuthInterceptor.USER_ATTR) User admin) {
        return userService.approve(id, admin);
    }

    @PutMapping("/users/{id}/reject")
    public UserResponse reject(@PathVariable("id") Long id,
                               @RequestAttribute(AuthInterceptor.USER_ATTR) User admin,
                               @RequestBody(required = false) DecisionRequest body) {
        return userService.reject(id, admin, body == null ? null : body.reason());
    }

    @PutMapping("/users/{id}/role")
    public UserResponse changeRole(@PathVariable("id") Long id,
                                   @RequestAttribute(AuthInterceptor.USER_ATTR) User admin,
                                   @Valid @RequestBody RoleChangeRequest body) {
        return userService.changeRole(id, admin, body);
    }

    @PutMapping("/users/{id}/status")
    public UserResponse changeStatus(@PathVariable("id") Long id,
                                     @RequestAttribute(AuthInterceptor.USER_ATTR) User admin,
                                     @Valid @RequestBody StatusChangeRequest body) {
        return userService.changeStatus(id, admin, body);
    }

    @DeleteMapping("/users/{id}")
    public Map<String, String> delete(@PathVariable("id") Long id,
                                      @RequestAttribute(AuthInterceptor.USER_ATTR) User admin,
                                      @RequestParam(name = "reason") String reason) {
        userService.delete(id, admin, reason);
        return Map.of("message", "Account deleted. Login access removed; audit records retained.");
    }

    @PostMapping("/users/{id}/force-logout")
    public Map<String, String> forceLogout(@PathVariable("id") Long id,
                                           @RequestAttribute(AuthInterceptor.USER_ATTR) User admin) {
        userService.forceLogout(id, admin);
        return Map.of("message", "User's sessions ended");
    }

    @GetMapping("/audit-logs")
    public List<AuditLog> auditLogs(@RequestParam(name = "action", required = false) String action) {
        return audit.list(action);
    }
}