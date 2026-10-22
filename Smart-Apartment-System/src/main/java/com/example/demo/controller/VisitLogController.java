package com.example.demo.controller;

import com.example.demo.config.AuthInterceptor;
import com.example.demo.config.RequireRoles;
import com.example.demo.dto.VisitDtos.*;
import com.example.demo.model.Role;
import com.example.demo.model.User;
import com.example.demo.service.VisitService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/visit-logs")
public class VisitLogController {
    private final VisitService service;

    public VisitLogController(VisitService service) { this.service = service; }

    @RequireRoles({Role.RESIDENT, Role.SECURITY_GUARD, Role.APARTMENT_MANAGER, Role.ADMINISTRATOR})
    @GetMapping
    public List<VisitResponse> logs(@RequestAttribute(AuthInterceptor.USER_ATTR) User user,
                                    @RequestParam(name = "status", required = false) String status) {
        return service.logs(user, status);
    }

    @RequireRoles({Role.SECURITY_GUARD, Role.APARTMENT_MANAGER, Role.ADMINISTRATOR})
    @GetMapping("/inside")
    public List<VisitResponse> inside() { return service.inside(); }

    @RequireRoles(Role.SECURITY_GUARD)
    @PostMapping("/{id}/entry")
    public VisitResponse entry(@RequestAttribute(AuthInterceptor.USER_ATTR) User user, @PathVariable("id") Long id) {
        return service.recordEntry(user, id);
    }

    @RequireRoles(Role.SECURITY_GUARD)
    @PostMapping("/{id}/exit")
    public VisitResponse exit(@RequestAttribute(AuthInterceptor.USER_ATTR) User user, @PathVariable("id") Long id) {
        return service.recordExit(user, id);
    }

    @RequireRoles(Role.ADMINISTRATOR)
    @PutMapping("/{id}/entry")
    public VisitResponse correctEntry(@RequestAttribute(AuthInterceptor.USER_ATTR) User user, @PathVariable("id") Long id,
                                      @Valid @RequestBody TimeCorrectionRequest d) {
        return service.correctEntry(user, id, d);
    }

    @RequireRoles(Role.ADMINISTRATOR)
    @DeleteMapping("/{id}/entry")
    public VisitResponse deleteEntry(@RequestAttribute(AuthInterceptor.USER_ATTR) User user, @PathVariable("id") Long id,
                                     @RequestParam(name = "reason") String reason) {
        return service.deleteEntry(user, id, reason);
    }

    @RequireRoles(Role.ADMINISTRATOR)
    @PutMapping("/{id}/exit")
    public VisitResponse correctExit(@RequestAttribute(AuthInterceptor.USER_ATTR) User user, @PathVariable("id") Long id,
                                     @Valid @RequestBody TimeCorrectionRequest d) {
        return service.correctExit(user, id, d);
    }

    @RequireRoles(Role.ADMINISTRATOR)
    @DeleteMapping("/{id}/exit")
    public VisitResponse deleteExit(@RequestAttribute(AuthInterceptor.USER_ATTR) User user, @PathVariable("id") Long id,
                                    @RequestParam(name = "reason") String reason) {
        return service.deleteExit(user, id, reason);
    }
}