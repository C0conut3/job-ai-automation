package com.jobai.automation.applicationrecord.web.dto;

import java.time.Instant;

public record ApplicationRecordDto(
        Long id,
        Long jobId,
        String jobTitle,
        String companyName,
        Long seekerUserId,
        String seekerName,
        String status,
        Instant appliedAt,
        Instant updatedAt) {}