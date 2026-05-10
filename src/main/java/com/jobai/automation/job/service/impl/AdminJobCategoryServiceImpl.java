package com.jobai.automation.job.service.impl;

import com.jobai.automation.common.exception.ApiException;
import com.jobai.automation.job.domain.JobCategory;
import com.jobai.automation.job.repository.JobCategoryRepository;
import com.jobai.automation.job.service.AdminJobCategoryService;
import com.jobai.automation.job.web.dto.JobCategoryResponse;
import com.jobai.automation.job.web.dto.JobCategoryWriteRequest;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminJobCategoryServiceImpl implements AdminJobCategoryService {

    private final JobCategoryRepository jobCategoryRepository;

    public AdminJobCategoryServiceImpl(JobCategoryRepository jobCategoryRepository) {
        this.jobCategoryRepository = jobCategoryRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<JobCategoryResponse> listAll() {
        return jobCategoryRepository.findAllByOrderBySortOrderAscIdAsc().stream().map(AdminJobCategoryServiceImpl::toResponse).toList();
    }

    @Override
    @Transactional
    public JobCategoryResponse create(JobCategoryWriteRequest request) {
        if (jobCategoryRepository.existsByNameIgnoreCase(request.name().trim())) {
            throw new ApiException(HttpStatus.CONFLICT, "类别名称已存在");
        }
        JobCategory c = new JobCategory();
        c.setName(request.name().trim());
        c.setDescription(normalizeDescription(request.description()));
        c.setSortOrder(request.sortOrder() != null ? request.sortOrder() : 0);
        jobCategoryRepository.save(c);
        return toResponse(c);
    }

    @Override
    @Transactional
    public JobCategoryResponse update(Long id, JobCategoryWriteRequest request) {
        JobCategory c = jobCategoryRepository.findById(id).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "类别不存在"));
        String newName = request.name().trim();
        if (!c.getName().equalsIgnoreCase(newName)
                && jobCategoryRepository.existsByNameIgnoreCase(newName)) {
            throw new ApiException(HttpStatus.CONFLICT, "类别名称已存在");
        }
        c.setName(newName);
        c.setDescription(normalizeDescription(request.description()));
        if (request.sortOrder() != null) {
            c.setSortOrder(request.sortOrder());
        }
        jobCategoryRepository.save(c);
        return toResponse(c);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        if (!jobCategoryRepository.existsById(id)) {
            throw new ApiException(HttpStatus.NOT_FOUND, "类别不存在");
        }
        jobCategoryRepository.deleteById(id);
    }

    private static JobCategoryResponse toResponse(JobCategory c) {
        return new JobCategoryResponse(c.getId(), c.getName(), c.getDescription(), c.getSortOrder(), c.getCreatedAt());
    }

    private static String normalizeDescription(String raw) {
        if (raw == null) {
            return null;
        }
        String t = raw.trim();
        return t.isEmpty() ? null : t;
    }
}
