package com.example.demo.controller;

import com.example.demo.config.AuthInterceptor;
import com.example.demo.config.RequireRoles;
import com.example.demo.dto.ResidentDtos.*;
import com.example.demo.model.AuditLog;
import com.example.demo.model.EditHistory;
import com.example.demo.model.Role;
import com.example.demo.model.User;
import com.example.demo.service.ResidentService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/residents")
public class ResidentController {
    private final ResidentService service;

    public ResidentController(ResidentService service) { this.service = service; }

    @RequireRoles(Role.ADMINISTRATOR)
    @PostMapping
    public ResponseEntity<Map<String, Object>> add(@RequestAttribute(AuthInterceptor.USER_ATTR) User user,
                                                   @Valid @RequestBody AddResidentRequest d) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.add(user, d));
    }

    @RequireRoles({Role.ADMINISTRATOR, Role.SECURITY_GUARD, Role.APARTMENT_MANAGER})
    @GetMapping
    public List<Object> directory(@RequestAttribute(AuthInterceptor.USER_ATTR) User user,
                                  @RequestParam(name = "unit", required = false) String unit,
                                  @RequestParam(name = "name", required = false) String name,
                                  @RequestParam(name = "block", required = false) String block) {
        return service.directory(user, unit, name, block);
    }

    @RequireRoles({Role.RESIDENT, Role.ADMINISTRATOR})
    @GetMapping("/me")
    public ResidentFullView me(@RequestAttribute(AuthInterceptor.USER_ATTR) User user) {
        return service.myProfile(user);
    }

    @RequireRoles({Role.APARTMENT_MANAGER, Role.ADMINISTRATOR})
    @GetMapping("/export")
    public ResponseEntity<String> export(@RequestAttribute(AuthInterceptor.USER_ATTR) User user) {
        return ResponseEntity.ok()
                .header("Content-Type", "text/csv")
                .header("Content-Disposition", "attachment; filename=residents.csv")
                .body(service.exportCsv(user));
    }

    @RequireRoles({Role.APARTMENT_MANAGER, Role.ADMINISTRATOR})
    @GetMapping("/vacant-units")
    public List<String> vacantUnits() { return service.vacantUnits(); }

    @RequireRoles({Role.APARTMENT_MANAGER, Role.ADMINISTRATOR})
    @GetMapping("/deletion-history")
    public List<AuditLog> deletionHistory() { return service.deletionHistory(); }

    @RequireRoles(Role.MAINTENANCE_STAFF)
    @GetMapping("/unit/{unit}")
    public UnitOnlyView unit(@RequestAttribute(AuthInterceptor.USER_ATTR) User user, @PathVariable("unit") String unit) {
        return service.unitForComplaint(user, unit);
    }

    @RequireRoles({Role.RESIDENT, Role.APARTMENT_MANAGER, Role.ADMINISTRATOR})
    @GetMapping("/{id}")
    public Object get(@RequestAttribute(AuthInterceptor.USER_ATTR) User user, @PathVariable("id") Long id) {
        return service.get(user, id);
    }

    @RequireRoles({Role.RESIDENT, Role.APARTMENT_MANAGER, Role.ADMINISTRATOR})
    @GetMapping("/{id}/history")
    public List<EditHistory> history(@RequestAttribute(AuthInterceptor.USER_ATTR) User user, @PathVariable("id") Long id) {
        return service.history(user, id);
    }

    @RequireRoles(Role.ADMINISTRATOR)
    @PutMapping("/{id}")
    public ResidentFullView edit(@RequestAttribute(AuthInterceptor.USER_ATTR) User user, @PathVariable("id") Long id,
                                 @Valid @RequestBody EditResidentRequest d) {
        return service.edit(user, id, d);
    }

    @RequireRoles(Role.ADMINISTRATOR)
    @PostMapping("/{id}/notify-removal")
    public Map<String, String> notifyRemoval(@RequestAttribute(AuthInterceptor.USER_ATTR) User user,
                                             @PathVariable("id") Long id) {
        service.notifyRemoval(user, id);
        return Map.of("message", "Resident notified of the scheduled removal");
    }

    @RequireRoles(Role.ADMINISTRATOR)
    @DeleteMapping("/{id}")
    public Map<String, String> delete(@RequestAttribute(AuthInterceptor.USER_ATTR) User user, @PathVariable("id") Long id,
                                      @RequestParam(name = "reason") String reason,
                                      @RequestParam(name = "confirmMoveOut", defaultValue = "false") boolean confirmMoveOut) {
        service.delete(user, id, reason, confirmMoveOut);
        return Map.of("message", "Resident removed. Login revoked, record kept in deletion history, unit available for reassignment.");
    }

    @RequireRoles(Role.ADMINISTRATOR)
    @PostMapping("/{id}/restore")
    public ResidentFullView restore(@RequestAttribute(AuthInterceptor.USER_ATTR) User user, @PathVariable("id") Long id) {
        return service.restore(user, id);
    }
}