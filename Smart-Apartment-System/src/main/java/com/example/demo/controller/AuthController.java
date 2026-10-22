package com.example.demo.controller;

import com.example.demo.config.AuthInterceptor;
import com.example.demo.dto.Dtos.*;
import com.example.demo.model.*;
import com.example.demo.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService authService;

    public AuthController(AuthService authService) { this.authService = authService; }

    @PostMapping("/register")
    public ResponseEntity<Map<String, Object>> register(@Valid @RequestBody RegisterRequest r) {
        User u = authService.register(r);
        String msg = u.getStatus() == AccountStatus.ACTIVE
                ? "Registration successful. Your account is active."
                : "Registration successful. Your account is pending Administrator approval.";
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("message", msg, "user", UserResponse.from(u)));
    }

    @PostMapping("/register/visitor")
    public ResponseEntity<Map<String, Object>> registerVisitor(@Valid @RequestBody VisitorRequest d) {
        VisitRequest v = authService.registerVisitor(d);
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                "message", "Visit request recorded and sent to the resident for approval. No login account is needed.",
                "requestId", v.getId(), "status", v.getStatus()));
    }

    @PostMapping("/login")
    public Map<String, Object> login(@Valid @RequestBody LoginRequest r) { return authService.login(r); }

    @PostMapping("/logout")
    public Map<String, String> logout(@RequestAttribute(AuthInterceptor.SESSION_ATTR) UserSession session) {
        authService.logout(session);
        return Map.of("message", "Logged out successfully");
    }

    @PostMapping("/logout-all")
    public Map<String, String> logoutAll(@RequestAttribute(AuthInterceptor.USER_ATTR) User user) {
        authService.revokeAllSessions(user.getId(), "LOGOUT_ALL");
        return Map.of("message", "All sessions ended");
    }

    @GetMapping("/sessions")
    public List<SessionResponse> sessions(@RequestAttribute(AuthInterceptor.USER_ATTR) User user,
                                          @RequestAttribute(AuthInterceptor.SESSION_ATTR) UserSession session) {
        return authService.listSessions(user.getId(), session.getId());
    }

    @PostMapping("/change-password")
    public Map<String, String> changePassword(@RequestAttribute(AuthInterceptor.USER_ATTR) User user,
                                              @RequestAttribute(AuthInterceptor.SESSION_ATTR) UserSession session,
                                              @Valid @RequestBody ChangePasswordRequest r) {
        authService.changePassword(user, session, r);
        return Map.of("message", "Password changed. Other sessions were logged out.");
    }
}