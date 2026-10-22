package com.example.demo.controller;

import com.example.demo.config.AuthInterceptor;
import com.example.demo.config.RequireRoles;
import com.example.demo.dto.VisitDtos.VisitResponse;
import com.example.demo.model.Role;
import com.example.demo.model.User;
import com.example.demo.service.SearchService;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/search")
public class SearchController {
    private final SearchService service;

    public SearchController(SearchService service) { this.service = service; }

    @RequireRoles({Role.RESIDENT, Role.SECURITY_GUARD, Role.APARTMENT_MANAGER, Role.ADMINISTRATOR})
    @GetMapping("/visitors")
    public List<VisitResponse> visitors(@RequestAttribute(AuthInterceptor.USER_ATTR) User user,
                                        @RequestParam(name = "q", required = false) String q,
                                        @RequestParam(name = "status", required = false) String status,
                                        @RequestParam(name = "unit", required = false) String unit,
                                        @RequestParam(name = "from", required = false) String from,
                                        @RequestParam(name = "to", required = false) String to,
                                        @RequestParam(name = "preRegistered", required = false) Boolean preRegistered) {
        return service.visitors(user, q, status, unit, from, to, preRegistered);
    }

    @RequireRoles({Role.RESIDENT, Role.MAINTENANCE_STAFF, Role.APARTMENT_MANAGER, Role.ADMINISTRATOR})
    @GetMapping("/complaints")
    public List<Object> complaints(@RequestAttribute(AuthInterceptor.USER_ATTR) User user,
                                   @RequestParam(name = "q", required = false) String q,
                                   @RequestParam(name = "status", required = false) String status,
                                   @RequestParam(name = "category", required = false) String category,
                                   @RequestParam(name = "unit", required = false) String unit,
                                   @RequestParam(name = "from", required = false) String from,
                                   @RequestParam(name = "to", required = false) String to,
                                   @RequestParam(name = "assignedTo", required = false) Long assignedTo) {
        return service.complaints(user, q, status, category, unit, from, to, assignedTo);
    }
}