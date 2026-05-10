package com.jobai.automation.job.service.impl;

import com.jobai.automation.applicationrecord.repository.JobApplicationRecordRepository;
import com.jobai.automation.common.api.PagedResponse;
import com.jobai.automation.common.exception.ApiException;
import com.jobai.automation.job.domain.Job;
import com.jobai.automation.job.domain.JobCategory;
import com.jobai.automation.job.domain.JobStatus;
import com.jobai.automation.job.domain.WorkType;
import com.jobai.automation.job.repository.JobCategoryRepository;
import com.jobai.automation.job.repository.JobRepository;
import com.jobai.automation.job.service.AdminJobService;
import com.jobai.automation.job.support.JobMappers;
import com.jobai.automation.job.web.dto.AdminJobWriteRequest;
import com.jobai.automation.job.web.dto.JobDetailResponse;
import com.jobai.automation.job.web.dto.JobSummaryResponse;
import com.jobai.automation.user.repository.UserRepository;
import java.time.Instant;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminJobServiceImpl implements AdminJobService {

    private final JobRepository jobRepository;
    private final JobCategoryRepository jobCategoryRepository;
    private final JobApplicationRecordRepository jobApplicationRecordRepository;
    private final UserRepository userRepository;

    public AdminJobServiceImpl(
            JobRepository jobRepository,
            JobCategoryRepository jobCategoryRepository,
            JobApplicationRecordRepository jobApplicationRecordRepository,
            UserRepository userRepository) {
        this.jobRepository = jobRepository;
        this.jobCategoryRepository = jobCategoryRepository;
        this.jobApplicationRecordRepository = jobApplicationRecordRepository;
        this.userRepository = userRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<JobSummaryResponse> listAll(Pageable pageable) {
        Page<Job> page = jobRepository.findAllPaged(pageable);
        return PagedResponse.of(page.map(JobMappers::toSummary));
    }

    @Override
    @Transactional(readOnly = true)
    public JobDetailResponse get(Long jobId) {
        Job job = jobRepository.findDetailById(jobId).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "职位不存在"));
        return JobMappers.toDetail(job);
    }

    @Override
    @Transactional
    public JobDetailResponse create(AdminJobWriteRequest request) {
        if (!userRepository.existsById(request.recruiterUserId())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "招聘方用户不存在");
        }
        Job job = new Job();
        job.setRecruiterUserId(request.recruiterUserId());
        applyAdminWrite(job, request);
        jobRepository.save(job);
        return JobMappers.toDetail(job);
    }

    @Override
    @Transactional
    public JobDetailResponse update(Long jobId, AdminJobWriteRequest request) {
        Job job = jobRepository.findDetailById(jobId).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "职位不存在"));
        if (!userRepository.existsById(request.recruiterUserId())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "招聘方用户不存在");
        }
        job.setRecruiterUserId(request.recruiterUserId());
        applyAdminWrite(job, request);
        jobRepository.save(job);
        return JobMappers.toDetail(job);
    }

    @Override
    @Transactional
    public void delete(Long jobId) {
        if (!jobRepository.existsById(jobId)) {
            throw new ApiException(HttpStatus.NOT_FOUND, "职位不存在");
        }
        jobApplicationRecordRepository.deleteByJob_Id(jobId);
        jobRepository.deleteById(jobId);
    }

    private void applyAdminWrite(Job job, AdminJobWriteRequest request) {
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
        job.setStatus(request.status());
        if (request.status() == JobStatus.PUBLISHED && job.getPublishedAt() == null) {
            job.setPublishedAt(Instant.now());
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
