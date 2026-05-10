package com.jobai.automation.applicationrecord.web;

import com.jobai.automation.applicationrecord.service.ApplicationRecordService;
import com.jobai.automation.applicationrecord.web.dto.ApplicationRecordDto;
import com.jobai.automation.common.api.ApiResponse;
import com.jobai.automation.user.UserRole;
import com.jobai.automation.user.support.AuthSessionSupport;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/application-records")
public class ApplicationRecordController {

    private final ApplicationRecordService applicationRecordService;

    public ApplicationRecordController(ApplicationRecordService applicationRecordService) {
        this.applicationRecordService = applicationRecordService;
    }

    @GetMapping("/health")
    public ApiResponse<String> health() {
        return ApiResponse.ok("application-record-module");
    }

    @GetMapping("/seeker")
    public ApiResponse<List<ApplicationRecordDto>> getSeekerRecords(HttpServletRequest request) {
        Long userId = AuthSessionSupport.requireUserId(request);
        AuthSessionSupport.requireActiveRole(request, UserRole.SEEKER);
        return ApiResponse.ok(applicationRecordService.getSeekerRecords(userId));
    }

    @GetMapping("/recruiter")
    public ApiResponse<List<ApplicationRecordDto>> getRecruiterRecords(HttpServletRequest request) {
        Long userId = AuthSessionSupport.requireUserId(request);
        AuthSessionSupport.requireActiveRole(request, UserRole.RECRUITER);
        return ApiResponse.ok(applicationRecordService.getRecruiterRecords(userId));
    }
}
