package com.example.demo.controller;

import com.example.demo.config.AuthInterceptor;
import com.example.demo.config.RequireRoles;
import com.example.demo.dto.VisitDtos.*;
import com.example.demo.model.Role;
import com.example.demo.model.User;
import com.example.demo.model.VisitRequest;
import com.example.demo.service.VisitService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/visitors")
public class VisitorController {
    private final VisitService service;

    public VisitorController(VisitService service) { this.service = service; }

    @RequireRoles({Role.SECURITY_GUARD, Role.ADMINISTRATOR})
    @PostMapping
    public ResponseEntity<Map<String, Object>> register(@RequestAttribute(AuthInterceptor.USER_ATTR) User user,
                                                        @Valid @RequestBody GateVisitorRequest d) {
        VisitRequest v = service.registerAtGate(user, d);
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                "message", "Visitor recorded and forwarded to the resident for approval",
                "visit", VisitResponse.from(v)));
    }

    @RequireRoles(Role.RESIDENT)
    @PostMapping("/pre-register")
    public ResponseEntity<Map<String, Object>> preRegister(@RequestAttribute(AuthInterceptor.USER_ATTR) User user,
                                                           @Valid @RequestBody PreRegisterRequest d) {
        VisitRequest v = service.preRegister(user, d);
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                "message", "Visitor pre-registered for the Security Guard to reference",
                "visit", VisitResponse.from(v)));
    }

    @RequireRoles({Role.RESIDENT, Role.SECURITY_GUARD, Role.APARTMENT_MANAGER, Role.ADMINISTRATOR})
    @GetMapping
    public Object list(@RequestAttribute(AuthInterceptor.USER_ATTR) User user,
                       @RequestParam(name = "status", required = false) String status) {
        List<VisitResponse> list = service.list(user, status);
        if (list.isEmpty() && user.getRole() == Role.RESIDENT)
            return Map.of("message", "No visitors yet. Use POST /api/visitors/pre-register to add an expected visitor.",
                    "visitors", list);
        return list;
    }

    @RequireRoles({Role.SECURITY_GUARD, Role.ADMINISTRATOR})
    @GetMapping("/pre-registered")
    public List<VisitResponse> preRegistered() { return service.preRegisteredList(); }

    @RequireRoles({Role.RESIDENT, Role.SECURITY_GUARD, Role.APARTMENT_MANAGER, Role.ADMINISTRATOR})
    @GetMapping("/{id}")
    public VisitResponse get(@RequestAttribute(AuthInterceptor.USER_ATTR) User user, @PathVariable("id") Long id) {
        return service.get(user, id);
    }

    @RequireRoles(Role.SECURITY_GUARD)
    @PutMapping("/{id}/confirm-arrival")
    public VisitResponse confirmArrival(@RequestAttribute(AuthInterceptor.USER_ATTR) User user,
                                        @PathVariable("id") Long id) {
        return service.confirmArrival(user, id);
    }

    @RequireRoles({Role.SECURITY_GUARD, Role.ADMINISTRATOR})
    @PutMapping("/{id}")
    public VisitResponse update(@RequestAttribute(AuthInterceptor.USER_ATTR) User user,
                                @PathVariable("id") Long id, @Valid @RequestBody UpdateVisitRequest d) {
        return service.update(user, id, d);
    }

    @RequireRoles(Role.ADMINISTRATOR)
    @DeleteMapping("/{id}")
    public Map<String, String> delete(@RequestAttribute(AuthInterceptor.USER_ATTR) User user,
                                      @PathVariable("id") Long id,
                                      @RequestParam(name = "reason") String reason) {
        service.delete(user, id, reason);
        return Map.of("message", "Visitor record deleted and the change logged");
    }
}