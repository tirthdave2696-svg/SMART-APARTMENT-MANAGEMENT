package com.example.demo.dto;

import com.example.demo.model.Notice;
import jakarta.validation.constraints.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

public final class NoticeDtos {
    private NoticeDtos() {}

    public record NoticeRequest(
            @NotBlank @Size(max = 200) String title,
            @NotBlank @Size(max = 2000) String message,
            String type, String audience, LocalDate expiresOn) {}

    public record EditNoticeRequest(
            @Size(max = 200) String title,
            @Size(max = 2000) String message,
            String type, String audience, LocalDate expiresOn) {}

    public record NoticeResponse(Long id, String title, String message, String type, String audience,
                                 LocalDate expiresOn, boolean expired, String createdByName,
                                 LocalDateTime createdAt, LocalDateTime updatedAt) {
        public static NoticeResponse from(Notice n) {
            boolean expired = n.getExpiresOn() != null && n.getExpiresOn().isBefore(LocalDate.now());
            return new NoticeResponse(n.getId(), n.getTitle(), n.getMessage(), n.getNoticeType(), n.getAudience(),
                    n.getExpiresOn(), expired, n.getCreatedByName(), n.getCreatedAt(), n.getUpdatedAt());
        }
    }
}