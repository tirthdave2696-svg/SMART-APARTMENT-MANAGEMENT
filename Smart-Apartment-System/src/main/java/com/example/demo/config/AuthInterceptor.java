package com.example.demo.config;

import com.example.demo.exception.ApiException;
import com.example.demo.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import java.util.Arrays;

@Component
public class AuthInterceptor implements HandlerInterceptor {
    public static final String USER_ATTR = "currentUser";
    public static final String SESSION_ATTR = "currentSession";

    private final AuthService authService;

    public AuthInterceptor(AuthService authService) { this.authService = authService; }

    @Override
    public boolean preHandle(HttpServletRequest req, HttpServletResponse res, Object handler) {
        if (!(handler instanceof HandlerMethod hm)) return true;

        String header = req.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer "))
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Authentication required. Please log in.");

        AuthService.AuthContext ctx = authService.authenticate(header.substring(7).trim());

        RequireRoles rr = hm.getMethodAnnotation(RequireRoles.class);
        if (rr == null) rr = hm.getBeanType().getAnnotation(RequireRoles.class);
        if (rr != null && !Arrays.asList(rr.value()).contains(ctx.user().getRole()))
            throw new ApiException(HttpStatus.FORBIDDEN, "Access denied: your role cannot use this feature.");

        req.setAttribute(USER_ATTR, ctx.user());
        req.setAttribute(SESSION_ATTR, ctx.session());
        return true;
    }
}