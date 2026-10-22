package com.example.demo.service;

import com.example.demo.model.AuditLog;
import com.example.demo.model.User;
import com.example.demo.repository.AuditLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class AuditService {
    private static final Logger log = LoggerFactory.getLogger(AuditService.class);
    private final AuditLogRepository repo;

    public AuditService(AuditLogRepository repo) { this.repo = repo; }

    public void log(Long actorId, String action, Long targetUserId, String details) {
        repo.save(new AuditLog(actorId, action, targetUserId, details));
    }

    /** Notification is simulated by logging; plug in email/SMS later. */
    public void notifyUser(User u, String message) {
        log.info("NOTIFICATION to {} / {}: {}", u.getEmail(), u.getPhone(), message);
    }

    public void securityAlert(String message) {
        log.warn("SECURITY ALERT: {}", message);
        repo.save(new AuditLog(null, "SECURITY_ALERT", null, message));
    }

    public List<AuditLog> list(String action) {
        return (action == null || action.isBlank())
                ? repo.findAllByOrderByCreatedAtDesc()
                : repo.findByActionOrderByCreatedAtDesc(action);
    }
}