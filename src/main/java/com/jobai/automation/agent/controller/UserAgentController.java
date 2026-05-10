package com.jobai.automation.agent.controller;

import com.jobai.automation.common.api.ApiResponse;
import com.jobai.automation.agent.dto.AgentRequest;
import com.jobai.automation.agent.dto.AgentResponse;
import com.jobai.automation.agent.service.UserAgentService;
import com.jobai.automation.user.UserRole;
import com.jobai.automation.user.support.AuthSessionSupport;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/agent")
public class UserAgentController {

    private final UserAgentService userAgentService;

    public UserAgentController(UserAgentService userAgentService) {
        this.userAgentService = userAgentService;
    }

    @PostMapping("/query")
    public ApiResponse<AgentResponse> query(@RequestBody AgentRequest request, HttpServletRequest servletRequest) {
        // Extract current user id from session. If absent, proceed with provided request.userId.
        try {
            Long currentUserId = AuthSessionSupport.requireUserId(servletRequest);
            // For resume-related actions require seeker role
            AuthSessionSupport.requireActiveRole(servletRequest, UserRole.SEEKER);
            if (request.userId() == null || request.userId().isBlank()) {
                request = new AgentRequest(String.valueOf(currentUserId), request.category(), request.message(), request.model());
            }
        } catch (Exception ignore) {
            // If no session or not seeker, leave request as-is (classification still works without resume)
        }
        return ApiResponse.ok(userAgentService.handle(request));
    }
}
