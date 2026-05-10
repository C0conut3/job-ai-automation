package com.jobai.automation.job.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record JobCategoryWriteRequest(
        @NotBlank @Size(max = 128) String name,
        @Size(max = 512) String description,
        Integer sortOrder) {}
