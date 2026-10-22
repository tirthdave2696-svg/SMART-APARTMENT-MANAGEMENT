package com.example.demo.controller;

import com.example.demo.config.AuthInterceptor;
import com.example.demo.config.RequireRoles;
import com.example.demo.dto.Dtos.*;
import com.example.demo.model.Role;
import com.example.demo.model.User;
import com.example.demo.service.UserService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
public class UserController {
    private final UserService userService;

    public UserController(UserService userService) { this.userService = userService; }

    @GetMapping("/me")
    public UserResponse me(@RequestAttribute(AuthInterceptor.USER_ATTR) User user) {
        return UserResponse.from(user);
    }

    @PutMapping("/me")
    public UserResponse update(@RequestAttribute(AuthInterceptor.USER_ATTR) User user,
                               @Valid @RequestBody UpdateProfileRequest d) {
        return userService.updateProfile(user, d);
    }

    @RequireRoles(Role.RESIDENT)
    @PostMapping("/me/correction-requests")
    public Map<String, String> correction(@RequestAttribute(AuthInterceptor.USER_ATTR) User user,
                                          @Valid @RequestBody CorrectionRequest d) {
        userService.requestCorrection(user, d.message());
        return Map.of("message", "Request sent to the Administrator");
    }
}