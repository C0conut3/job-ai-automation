package com.jobai.automation.job.service;

import com.jobai.automation.common.api.PagedResponse;
import com.jobai.automation.job.web.dto.JobDetailResponse;
import com.jobai.automation.job.web.dto.JobSummaryResponse;
import com.jobai.automation.job.web.dto.JobWriteRequest;
import org.springframework.data.domain.Pageable;

public interface RecruiterJobService {

    PagedResponse<JobSummaryResponse> listMine(Long recruiterUserId, Pageable pageable);

    JobDetailResponse getOwn(Long recruiterUserId, Long jobId);

    JobDetailResponse create(Long recruiterUserId, JobWriteRequest request);

    JobDetailResponse update(Long recruiterUserId, Long jobId, JobWriteRequest request);

    void withdraw(Long recruiterUserId, Long jobId);
}
