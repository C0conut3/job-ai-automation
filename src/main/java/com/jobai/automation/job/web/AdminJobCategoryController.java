package com.jobai.automation.job.web;

import com.jobai.automation.common.api.ApiResponse;
import com.jobai.automation.job.service.AdminJobCategoryService;
import com.jobai.automation.job.web.dto.JobCategoryResponse;
import com.jobai.automation.job.web.dto.JobCategoryWriteRequest;
import com.jobai.automation.user.UserRole;
import com.jobai.automation.user.support.AuthSessionSupport;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/job-categories")
public class AdminJobCategoryController {

    private final AdminJobCategoryService adminJobCategoryService;

    public AdminJobCategoryController(AdminJobCategoryService adminJobCategoryService) {
        this.adminJobCategoryService = adminJobCategoryService;
    }

    @GetMapping
    public ApiResponse<List<JobCategoryResponse>> list(HttpServletRequest request) {
        AuthSessionSupport.requireActiveRole(request, UserRole.ADMIN);
        return ApiResponse.ok(adminJobCategoryService.listAll());
    }

    @PostMapping
    public ApiResponse<JobCategoryResponse> create(@Valid @RequestBody JobCategoryWriteRequest body, HttpServletRequest request) {
        AuthSessionSupport.requireActiveRole(request, UserRole.ADMIN);
        return ApiResponse.ok(adminJobCategoryService.create(body));
    }

    @PutMapping("/{id}")
    public ApiResponse<JobCategoryResponse> update(
            @PathVariable Long id, @Valid @RequestBody JobCategoryWriteRequest body, HttpServletRequest request) {
        AuthSessionSupport.requireActiveRole(request, UserRole.ADMIN);
        return ApiResponse.ok(adminJobCategoryService.update(id, body));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id, HttpServletRequest request) {
        AuthSessionSupport.requireActiveRole(request, UserRole.ADMIN);
        adminJobCategoryService.delete(id);
        return ApiResponse.ok(null);
    }
}
