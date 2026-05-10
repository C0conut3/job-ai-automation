package com.jobai.automation.preference.web;

import com.jobai.automation.common.api.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/job-preferences")
public class JobPreferenceController {

    @GetMapping("/health")
    public ApiResponse<String> health() {
        return ApiResponse.ok("job-preference-module");
    }

    // TODO: 获取/更新偏好、与自动化任务绑定等 REST 接口
}
