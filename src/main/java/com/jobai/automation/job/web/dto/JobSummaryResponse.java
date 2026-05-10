package com.jobai.automation.job.web.dto;

import com.jobai.automation.job.domain.JobStatus;
import com.jobai.automation.job.domain.WorkType;
import java.time.Instant;

public record JobSummaryResponse(
        Long id,
        String title,
        String companyName,
        Long categoryId,
        String categoryName,
        Integer salaryMin,
        Integer salaryMax,
        String city,
        WorkType workType,
        JobStatus status,
        Instant publishedAt,
        Long recruiterUserId) {}
