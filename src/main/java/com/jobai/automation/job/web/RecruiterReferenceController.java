package com.jobai.automation.job.web;

import com.jobai.automation.common.api.ApiResponse;
import com.jobai.automation.job.service.AdminJobCategoryService;
import com.jobai.automation.job.web.dto.JobCategoryResponse;
import com.jobai.automation.user.UserRole;
import com.jobai.automation.user.support.AuthSessionSupport;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/recruiter")
public class RecruiterReferenceController {

    private final AdminJobCategoryService adminJobCategoryService;

    public RecruiterReferenceController(AdminJobCategoryService adminJobCategoryService) {
        this.adminJobCategoryService = adminJobCategoryService;
    }

    /** 发布职位时选择类别（只读列表）。 */
    @GetMapping("/job-categories")
    public ApiResponse<List<JobCategoryResponse>> listCategories(HttpServletRequest request) {
        AuthSessionSupport.requireActiveRole(request, UserRole.RECRUITER);
        return ApiResponse.ok(adminJobCategoryService.listAll());
    }
}
