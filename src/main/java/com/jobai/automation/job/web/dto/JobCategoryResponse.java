package com.jobai.automation.job.web.dto;

import java.time.Instant;

public record JobCategoryResponse(Long id, String name, String description, int sortOrder, Instant createdAt) {}
