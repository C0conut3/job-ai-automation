package com.jobai.automation.job.web;

import com.jobai.automation.common.api.ApiResponse;
import com.jobai.automation.common.api.PagedResponse;
import com.jobai.automation.job.service.SeekerJobService;
import com.jobai.automation.job.web.dto.JobDetailResponse;
import com.jobai.automation.job.web.dto.JobSearchCriteria;
import com.jobai.automation.job.web.dto.JobSummaryResponse;
import com.jobai.automation.user.UserRole;
import com.jobai.automation.user.support.AuthSessionSupport;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/seeker/jobs")
public class SeekerJobController {

    private final SeekerJobService seekerJobService;

    public SeekerJobController(SeekerJobService seekerJobService) {
        this.seekerJobService = seekerJobService;
    }

    @GetMapping
    public ApiResponse<PagedResponse<JobSummaryResponse>> listPublished(
            @PageableDefault(size = 10) Pageable pageable,
            @RequestParam(required = false) String title,
            @RequestParam(required = false) String city,
            @RequestParam(required = false) Integer salaryMin,
            @RequestParam(required = false) Integer salaryMax,
            @RequestParam(required = false) String workType,
            HttpServletRequest request) {
        Long userId = AuthSessionSupport.requireUserId(request);
        AuthSessionSupport.requireActiveRole(request, UserRole.SEEKER);
        JobSearchCriteria criteria = JobSearchCriteria.of(title, city, salaryMin, salaryMax, workType);
        return ApiResponse.ok(seekerJobService.listPublished(pageable, criteria, userId));
    }

    @GetMapping("/{id}")
    public ApiResponse<JobDetailResponse> detail(@PathVariable Long id, HttpServletRequest request) {
        AuthSessionSupport.requireActiveRole(request, UserRole.SEEKER);
        return ApiResponse.ok(seekerJobService.getPublished(id));
    }

    @PostMapping("/{id}/apply")
    public ApiResponse<Void> apply(@PathVariable Long id, HttpServletRequest request) {
        Long userId = AuthSessionSupport.requireUserId(request);
        AuthSessionSupport.requireActiveRole(request, UserRole.SEEKER);
        seekerJobService.apply(userId, id);
        return ApiResponse.ok(null);
    }
}
