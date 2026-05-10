package com.jobai.automation.resume.web.dto;

import java.time.Instant;

public record ResumeResponse(
        Long id,
        Long userId,
        String name,
        String personalInfo,
        String education,
        String jobStatus,
        String workExperience,
        String projectExperience,
        Instant createdAt,
        Instant updatedAt) {}
