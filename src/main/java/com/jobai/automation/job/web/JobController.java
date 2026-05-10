package com.jobai.automation.job.web;

import com.jobai.automation.common.api.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/jobs")
public class JobController {

    @GetMapping("/health")
    public ApiResponse<String> health() {
        return ApiResponse.ok("job-module");
    }

    // TODO: CRUD、搜索、与简历/偏好匹配等 REST 接口
}
