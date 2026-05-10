package com.jobai.automation.chat.web.dto;

import java.time.Instant;

public record ChatSessionDto(
        Long id,
        Long jobId,
        String jobTitle,
        Long seekerUserId,
        String seekerName,
        Long recruiterUserId,
        String recruiterName,
        Instant updatedAt,
        int unreadCount) {}