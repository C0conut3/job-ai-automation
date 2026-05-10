package com.jobai.automation.job.service;

import com.jobai.automation.common.api.PagedResponse;
import com.jobai.automation.job.web.dto.JobDetailResponse;
import com.jobai.automation.job.web.dto.JobSearchCriteria;
import com.jobai.automation.job.web.dto.JobSummaryResponse;
import org.springframework.data.domain.Pageable;

public interface SeekerJobService {

    PagedResponse<JobSummaryResponse> listPublished(Pageable pageable, JobSearchCriteria criteria, Long seekerUserId);

    JobDetailResponse getPublished(Long jobId);

    void apply(Long seekerUserId, Long jobId);
}
