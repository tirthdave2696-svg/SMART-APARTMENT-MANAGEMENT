package com.example.demo.controller;

import com.example.demo.config.AuthInterceptor;
import com.example.demo.config.RequireRoles;
import com.example.demo.dto.ComplaintDtos.*;
import com.example.demo.model.Role;
import com.example.demo.model.User;
import com.example.demo.service.ComplaintService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/complaints")
public class ComplaintController {
    private final ComplaintService service;

    public ComplaintController(ComplaintService service) { this.service = service; }

    @RequireRoles({Role.RESIDENT, Role.ADMINISTRATOR})
    @PostMapping
    public ResponseEntity<Map<String, Object>> raise(@RequestAttribute(AuthInterceptor.USER_ATTR) User user,
                                                     @Valid @RequestBody RaiseComplaintRequest d) {
        ComplaintResponse c = service.raise(user, d);
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                "message", "Complaint submitted with status OPEN", "complaint", c));
    }

    @RequireRoles({Role.RESIDENT, Role.MAINTENANCE_STAFF, Role.APARTMENT_MANAGER, Role.ADMINISTRATOR})
    @GetMapping
    public List<Object> list(@RequestAttribute(AuthInterceptor.USER_ATTR) User user,
                             @RequestParam(name = "status", required = false) String status,
                             @RequestParam(name = "category", required = false) String category) {
        return service.list(user, status, category);
    }

    @RequireRoles({Role.APARTMENT_MANAGER, Role.ADMINISTRATOR})
    @GetMapping("/summary")
    public Map<String, Object> summary() { return service.summary(); }

    @RequireRoles({Role.RESIDENT, Role.MAINTENANCE_STAFF, Role.APARTMENT_MANAGER, Role.ADMINISTRATOR})
    @GetMapping("/{id}")
    public Object get(@RequestAttribute(AuthInterceptor.USER_ATTR) User user, @PathVariable("id") Long id) {
        return service.get(user, id);
    }

    @RequireRoles({Role.RESIDENT, Role.ADMINISTRATOR})
    @PutMapping("/{id}")
    public ComplaintResponse edit(@RequestAttribute(AuthInterceptor.USER_ATTR) User user, @PathVariable("id") Long id,
                                  @Valid @RequestBody EditComplaintRequest d) {
        return service.edit(user, id, d);
    }

    @RequireRoles(Role.ADMINISTRATOR)
    @PutMapping("/{id}/assign")
    public ComplaintResponse assign(@RequestAttribute(AuthInterceptor.USER_ATTR) User user, @PathVariable("id") Long id,
                                    @Valid @RequestBody AssignRequest d) {
        return service.assign(user, id, d.staffId());
    }

    @RequireRoles({Role.MAINTENANCE_STAFF, Role.ADMINISTRATOR})
    @PutMapping("/{id}/status")
    public Object status(@RequestAttribute(AuthInterceptor.USER_ATTR) User user, @PathVariable("id") Long id,
                         @Valid @RequestBody ComplaintStatusRequest d) {
        return service.updateStatus(user, id, d);
    }
}