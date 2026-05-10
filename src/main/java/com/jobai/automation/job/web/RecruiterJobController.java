package com.jobai.automation.job.web;

import com.jobai.automation.common.api.ApiResponse;
import com.jobai.automation.common.api.PagedResponse;
import com.jobai.automation.job.service.RecruiterJobService;
import com.jobai.automation.job.web.dto.JobDetailResponse;
import com.jobai.automation.job.web.dto.JobSummaryResponse;
import com.jobai.automation.job.web.dto.JobWriteRequest;
import com.jobai.automation.user.UserRole;
import com.jobai.automation.user.support.AuthSessionSupport;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/recruiter/jobs")
public class RecruiterJobController {

    private final RecruiterJobService recruiterJobService;

    public RecruiterJobController(RecruiterJobService recruiterJobService) {
        this.recruiterJobService = recruiterJobService;
    }

    @GetMapping
    public ApiResponse<PagedResponse<JobSummaryResponse>> mine(
            @PageableDefault(size = 10) Pageable pageable, HttpServletRequest request) {
        Long userId = AuthSessionSupport.requireUserId(request);
        AuthSessionSupport.requireActiveRole(request, UserRole.RECRUITER);
        return ApiResponse.ok(recruiterJobService.listMine(userId, pageable));
    }

    @GetMapping("/{id}")
    public ApiResponse<JobDetailResponse> own(@PathVariable Long id, HttpServletRequest request) {
        Long userId = AuthSessionSupport.requireUserId(request);
        AuthSessionSupport.requireActiveRole(request, UserRole.RECRUITER);
        return ApiResponse.ok(recruiterJobService.getOwn(userId, id));
    }

    @PostMapping
    public ApiResponse<JobDetailResponse> create(
            @Valid @RequestBody JobWriteRequest body, HttpServletRequest request) {
        Long userId = AuthSessionSupport.requireUserId(request);
        AuthSessionSupport.requireActiveRole(request, UserRole.RECRUITER);
        return ApiResponse.ok(recruiterJobService.create(userId, body));
    }

    @PutMapping("/{id}")
    public ApiResponse<JobDetailResponse> update(
            @PathVariable Long id, @Valid @RequestBody JobWriteRequest body, HttpServletRequest request) {
        Long userId = AuthSessionSupport.requireUserId(request);
        AuthSessionSupport.requireActiveRole(request, UserRole.RECRUITER);
        return ApiResponse.ok(recruiterJobService.update(userId, id, body));
    }

    @PostMapping("/{id}/withdraw")
    public ApiResponse<Void> withdraw(@PathVariable Long id, HttpServletRequest request) {
        Long userId = AuthSessionSupport.requireUserId(request);
        AuthSessionSupport.requireActiveRole(request, UserRole.RECRUITER);
        recruiterJobService.withdraw(userId, id);
        return ApiResponse.ok(null);
    }
}
