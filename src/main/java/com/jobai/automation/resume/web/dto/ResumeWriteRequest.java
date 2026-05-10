package com.jobai.automation.resume.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ResumeWriteRequest(
        @NotBlank @Size(max = 128) String name,
        String personalInfo,
        String education,
        String jobStatus,
        String workExperience,
        String projectExperience) {}
