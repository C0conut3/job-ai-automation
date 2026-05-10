package com.jobai.automation.resume.web.dto;

public record ParsedResumeDto(
        String name,
        String personalInfo,
        String education,
        String jobStatus,
        String workExperience,
        String projectExperience) {}