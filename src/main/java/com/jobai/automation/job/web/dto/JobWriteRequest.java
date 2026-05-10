package com.jobai.automation.job.web.dto;

import com.jobai.automation.job.domain.JobStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record JobWriteRequest(
        @NotBlank @Size(max = 255) String title,
        @NotBlank @Size(max = 255) String companyName,
        @NotBlank String description,
        Long categoryId,
        Integer salaryMin,
        Integer salaryMax,
        @Size(max = 128) String city,
        String workType,
        JobStatus status) {}
