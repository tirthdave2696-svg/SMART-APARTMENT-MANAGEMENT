package com.example.demo.controller;

import com.example.demo.config.AuthInterceptor;
import com.example.demo.config.RequireRoles;
import com.example.demo.dto.Dtos.DecisionRequest;
import com.example.demo.dto.VisitDtos.*;
import com.example.demo.model.Role;
import com.example.demo.model.User;
import com.example.demo.service.VisitService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/visit-approvals")
public class VisitApprovalController {
    private final VisitService service;

    public VisitApprovalController(VisitService service) { this.service = service; }

    @RequireRoles({Role.RESIDENT, Role.SECURITY_GUARD, Role.APARTMENT_MANAGER, Role.ADMINISTRATOR})
    @GetMapping("/pending")
    public List<?> pending(@RequestAttribute(AuthInterceptor.USER_ATTR) User user) {
        return service.pending(user);
    }

    @RequireRoles({Role.RESIDENT, Role.SECURITY_GUARD, Role.APARTMENT_MANAGER, Role.ADMINISTRATOR})
    @GetMapping("/history")
    public List<VisitResponse> history(@RequestAttribute(AuthInterceptor.USER_ATTR) User user) {
        return service.history(user);
    }

    @RequireRoles({Role.RESIDENT, Role.SECURITY_GUARD, Role.ADMINISTRATOR})
    @GetMapping("/{id}/status")
    public VisitStatusView status(@RequestAttribute(AuthInterceptor.USER_ATTR) User user, @PathVariable("id") Long id) {
        return service.status(user, id);
    }

    @RequireRoles({Role.RESIDENT, Role.ADMINISTRATOR})
    @PutMapping("/{id}/approve")
    public VisitResponse approve(@RequestAttribute(AuthInterceptor.USER_ATTR) User user, @PathVariable("id") Long id,
                                 @RequestBody(required = false) DecisionRequest body) {
        return service.decide(user, id, true, body == null ? null : body.reason());
    }

    @RequireRoles({Role.RESIDENT, Role.ADMINISTRATOR})
    @PutMapping("/{id}/reject")
    public VisitResponse reject(@RequestAttribute(AuthInterceptor.USER_ATTR) User user, @PathVariable("id") Long id,
                                @RequestBody(required = false) DecisionRequest body) {
        return service.decide(user, id, false, body == null ? null : body.reason());
    }

    @RequireRoles(Role.ADMINISTRATOR)
    @PutMapping("/{id}/override")
    public VisitResponse override(@RequestAttribute(AuthInterceptor.USER_ATTR) User user, @PathVariable("id") Long id,
                                  @Valid @RequestBody OverrideRequest body) {
        return service.override(user, id, body.decision(), body.reason());
    }
}