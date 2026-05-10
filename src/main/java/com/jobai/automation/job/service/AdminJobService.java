package com.jobai.automation.job.service;

import com.jobai.automation.common.api.PagedResponse;
import com.jobai.automation.job.web.dto.AdminJobWriteRequest;
import com.jobai.automation.job.web.dto.JobDetailResponse;
import com.jobai.automation.job.web.dto.JobSummaryResponse;
import org.springframework.data.domain.Pageable;

public interface AdminJobService {

    PagedResponse<JobSummaryResponse> listAll(Pageable pageable);

    JobDetailResponse get(Long jobId);

    JobDetailResponse create(AdminJobWriteRequest request);

    JobDetailResponse update(Long jobId, AdminJobWriteRequest request);

    void delete(Long jobId);
}
