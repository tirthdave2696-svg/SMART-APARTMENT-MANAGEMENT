package com.example.demo.controller;

import com.example.demo.config.AuthInterceptor;
import com.example.demo.config.RequireRoles;
import com.example.demo.dto.NoticeDtos.*;
import com.example.demo.model.Role;
import com.example.demo.model.User;
import com.example.demo.service.NoticeService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notices")
public class NoticeController {
    private final NoticeService service;

    public NoticeController(NoticeService service) { this.service = service; }

    @RequireRoles(Role.ADMINISTRATOR)
    @PostMapping
    public ResponseEntity<Map<String, Object>> create(@RequestAttribute(AuthInterceptor.USER_ATTR) User user,
                                                      @Valid @RequestBody NoticeRequest d) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(user, d));
    }

    @RequireRoles({Role.RESIDENT, Role.SECURITY_GUARD, Role.MAINTENANCE_STAFF, Role.APARTMENT_MANAGER, Role.ADMINISTRATOR})
    @GetMapping
    public List<NoticeResponse> list(@RequestAttribute(AuthInterceptor.USER_ATTR) User user,
                                     @RequestParam(name = "type", required = false) String type) {
        return service.list(user, type);
    }

    @RequireRoles({Role.RESIDENT, Role.SECURITY_GUARD, Role.MAINTENANCE_STAFF, Role.APARTMENT_MANAGER, Role.ADMINISTRATOR})
    @GetMapping("/{id}")
    public NoticeResponse get(@RequestAttribute(AuthInterceptor.USER_ATTR) User user, @PathVariable("id") Long id) {
        return service.get(user, id);
    }

    @RequireRoles(Role.ADMINISTRATOR)
    @PutMapping("/{id}")
    public NoticeResponse edit(@RequestAttribute(AuthInterceptor.USER_ATTR) User user, @PathVariable("id") Long id,
                               @Valid @RequestBody EditNoticeRequest d) {
        return service.edit(user, id, d);
    }

    @RequireRoles(Role.ADMINISTRATOR)
    @DeleteMapping("/{id}")
    public Map<String, String> delete(@RequestAttribute(AuthInterceptor.USER_ATTR) User user, @PathVariable("id") Long id) {
        service.delete(user, id);
        return Map.of("message", "Notice deleted");
    }
}