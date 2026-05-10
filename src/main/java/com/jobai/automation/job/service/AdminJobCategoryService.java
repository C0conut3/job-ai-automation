package com.jobai.automation.job.service;

import com.jobai.automation.job.web.dto.JobCategoryResponse;
import com.jobai.automation.job.web.dto.JobCategoryWriteRequest;
import java.util.List;

public interface AdminJobCategoryService {

    List<JobCategoryResponse> listAll();

    JobCategoryResponse create(JobCategoryWriteRequest request);

    JobCategoryResponse update(Long id, JobCategoryWriteRequest request);

    void delete(Long id);
}
