package com.jobai.automation.job.support;

import com.jobai.automation.job.domain.Job;
import com.jobai.automation.job.web.dto.JobDetailResponse;
import com.jobai.automation.job.web.dto.JobSummaryResponse;

public final class JobMappers {

    private JobMappers() {}

    public static JobSummaryResponse toSummary(Job job) {
        Long catId = job.getCategory() != null ? job.getCategory().getId() : null;
        String catName = job.getCategory() != null ? job.getCategory().getName() : null;
        return new JobSummaryResponse(
                job.getId(),
                job.getTitle(),
                job.getCompanyName(),
                catId,
                catName,
                job.getSalaryMin(),
                job.getSalaryMax(),
                job.getCity(),
                job.getWorkType(),
                job.getStatus(),
                job.getPublishedAt(),
                job.getRecruiterUserId());
    }

    public static JobDetailResponse toDetail(Job job) {
        Long catId = job.getCategory() != null ? job.getCategory().getId() : null;
        String catName = job.getCategory() != null ? job.getCategory().getName() : null;
        return new JobDetailResponse(
                job.getId(),
                job.getTitle(),
                job.getCompanyName(),
                job.getDescription(),
                catId,
                catName,
                job.getSalaryMin(),
                job.getSalaryMax(),
                job.getCity(),
                job.getWorkType(),
                job.getStatus(),
                job.getPublishedAt(),
                job.getRecruiterUserId(),
                job.getCreatedAt(),
                job.getUpdatedAt());
    }
}
