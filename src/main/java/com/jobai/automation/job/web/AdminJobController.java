package com.jobai.automation.job.web;

import com.jobai.automation.common.api.ApiResponse;
import com.jobai.automation.common.api.PagedResponse;
import com.jobai.automation.job.service.AdminJobService;
import com.jobai.automation.job.web.dto.AdminJobWriteRequest;
import com.jobai.automation.job.web.dto.JobDetailResponse;
import com.jobai.automation.job.web.dto.JobSummaryResponse;
import com.jobai.automation.user.UserRole;
import com.jobai.automation.user.support.AuthSessionSupport;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/jobs")
public class AdminJobController {

    private final AdminJobService adminJobService;

    public AdminJobController(AdminJobService adminJobService) {
        this.adminJobService = adminJobService;
    }

    @GetMapping
    public ApiResponse<PagedResponse<JobSummaryResponse>> list(
            @PageableDefault(size = 10) Pageable pageable, HttpServletRequest request) {
        AuthSessionSupport.requireActiveRole(request, UserRole.ADMIN);
        return ApiResponse.ok(adminJobService.listAll(pageable));
    }

    @GetMapping("/{id}")
    public ApiResponse<JobDetailResponse> get(@PathVariable Long id, HttpServletRequest request) {
        AuthSessionSupport.requireActiveRole(request, UserRole.ADMIN);
        return ApiResponse.ok(adminJobService.get(id));
    }

    @PostMapping
    public ApiResponse<JobDetailResponse> create(@Valid @RequestBody AdminJobWriteRequest body, HttpServletRequest request) {
        AuthSessionSupport.requireActiveRole(request, UserRole.ADMIN);
        return ApiResponse.ok(adminJobService.create(body));
    }

    @PutMapping("/{id}")
    public ApiResponse<JobDetailResponse> update(
            @PathVariable Long id, @Valid @RequestBody AdminJobWriteRequest body, HttpServletRequest request) {
        AuthSessionSupport.requireActiveRole(request, UserRole.ADMIN);
        return ApiResponse.ok(adminJobService.update(id, body));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id, HttpServletRequest request) {
        AuthSessionSupport.requireActiveRole(request, UserRole.ADMIN);
        adminJobService.delete(id);
        return ApiResponse.ok(null);
    }
}
