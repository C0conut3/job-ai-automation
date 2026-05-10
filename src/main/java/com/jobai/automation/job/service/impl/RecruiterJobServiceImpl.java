package com.jobai.automation.job.service.impl;

import com.jobai.automation.common.api.PagedResponse;
import com.jobai.automation.common.exception.ApiException;
import com.jobai.automation.job.domain.Job;
import com.jobai.automation.job.domain.JobCategory;
import com.jobai.automation.job.domain.JobStatus;
import com.jobai.automation.job.domain.WorkType;
import com.jobai.automation.job.repository.JobCategoryRepository;
import com.jobai.automation.job.repository.JobRepository;
import com.jobai.automation.job.service.RecruiterJobService;
import com.jobai.automation.job.support.JobMappers;
import com.jobai.automation.job.web.dto.JobDetailResponse;
import com.jobai.automation.job.web.dto.JobSummaryResponse;
import com.jobai.automation.job.web.dto.JobWriteRequest;
import java.time.Instant;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RecruiterJobServiceImpl implements RecruiterJobService {

    private final JobRepository jobRepository;
    private final JobCategoryRepository jobCategoryRepository;

    public RecruiterJobServiceImpl(JobRepository jobRepository, JobCategoryRepository jobCategoryRepository) {
        this.jobRepository = jobRepository;
        this.jobCategoryRepository = jobCategoryRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<JobSummaryResponse> listMine(Long recruiterUserId, Pageable pageable) {
        Page<Job> page = jobRepository.findByRecruiterUserId(recruiterUserId, pageable);
        return PagedResponse.of(page.map(JobMappers::toSummary));
    }

    @Override
    @Transactional(readOnly = true)
    public JobDetailResponse getOwn(Long recruiterUserId, Long jobId) {
        Job job = loadOwnedOrThrow(recruiterUserId, jobId);
        return JobMappers.toDetail(job);
    }

    @Override
    @Transactional
    public JobDetailResponse create(Long recruiterUserId, JobWriteRequest request) {
        Job job = new Job();
        job.setRecruiterUserId(recruiterUserId);
        applyWrite(job, request);
        job.setStatus(JobStatus.PUBLISHED);
        job.setPublishedAt(Instant.now());
        jobRepository.save(job);
        return JobMappers.toDetail(job);
    }

    @Override
    @Transactional
    public JobDetailResponse update(Long recruiterUserId, Long jobId, JobWriteRequest request) {
        Job job = loadOwnedOrThrow(recruiterUserId, jobId);
        if (job.getStatus() == JobStatus.WITHDRAWN) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "已撤回的职位不可修改");
        }
        applyWrite(job, request);
        jobRepository.save(job);
        return JobMappers.toDetail(job);
    }

    @Override
    @Transactional
    public void withdraw(Long recruiterUserId, Long jobId) {
        Job job = loadOwnedOrThrow(recruiterUserId, jobId);
        job.setStatus(JobStatus.WITHDRAWN);
        jobRepository.save(job);
    }

    private Job loadOwnedOrThrow(Long recruiterUserId, Long jobId) {
        Job job = jobRepository.findDetailById(jobId).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "职位不存在"));
        if (!job.getRecruiterUserId().equals(recruiterUserId)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "无权操作该职位");
        }
        return job;
    }

    private void applyWrite(Job job, JobWriteRequest request) {
        job.setTitle(request.title());
        job.setCompanyName(request.companyName());
        job.setDescription(request.description());
        job.setSalaryMin(request.salaryMin());
        job.setSalaryMax(request.salaryMax());
        job.setCity(request.city());
        job.setWorkType(parseWorkType(request.workType()));
        if (request.categoryId() != null) {
            JobCategory cat =
                    jobCategoryRepository.findById(request.categoryId()).orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "职位类别不存在"));
            job.setCategory(cat);
        } else {
            job.setCategory(null);
        }
    }

    private static WorkType parseWorkType(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return WorkType.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "无效的工作类型: " + raw);
        }
    }
}
