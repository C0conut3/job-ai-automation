package com.jobai.automation.agent.controller;

import com.jobai.automation.agent.dto.AgentResponse;
import com.jobai.automation.agent.service.AgentChatService;
import com.jobai.automation.common.api.ApiResponse;
import com.jobai.automation.user.UserRole;
import com.jobai.automation.user.support.AuthSessionSupport;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/agent/chat")
public class AgentChatController {

    private final AgentChatService agentChatService;

    public AgentChatController(AgentChatService agentChatService) {
        this.agentChatService = agentChatService;
    }

    @GetMapping("/sessions")
    public ApiResponse<List<AgentChatService.SessionSummary>> getSessions(HttpServletRequest request) {
        Long userId = AuthSessionSupport.requireUserId(request);
        AuthSessionSupport.requireActiveRole(request, UserRole.SEEKER);
        return ApiResponse.ok(agentChatService.getUserSessions(userId));
    }

    @GetMapping("/sessions/{sessionId}")
    public ApiResponse<AgentChatService.SessionDetail> getSessionDetail(
            @PathVariable Long sessionId,
            HttpServletRequest request) {
        Long userId = AuthSessionSupport.requireUserId(request);
        AuthSessionSupport.requireActiveRole(request, UserRole.SEEKER);
        return ApiResponse.ok(agentChatService.getSessionDetail(userId, sessionId));
    }

    @PostMapping("/sessions")
    public ApiResponse<AgentResponse> createSession(@RequestBody ChatRequest requestBody, HttpServletRequest request) {
        Long userId = AuthSessionSupport.requireUserId(request);
        AuthSessionSupport.requireActiveRole(request, UserRole.SEEKER);
        return ApiResponse.ok(agentChatService.createSessionAndQuery(userId, requestBody.message()));
    }

    @PostMapping("/sessions/{sessionId}/messages")
    public ApiResponse<AgentResponse> sendMessage(
            @PathVariable Long sessionId,
            @RequestBody ChatRequest requestBody,
            HttpServletRequest request) {
        Long userId = AuthSessionSupport.requireUserId(request);
        AuthSessionSupport.requireActiveRole(request, UserRole.SEEKER);
        return ApiResponse.ok(agentChatService.query(userId, sessionId, requestBody.message()));
    }

    @DeleteMapping("/sessions/{sessionId}")
    public ApiResponse<Void> deleteSession(@PathVariable Long sessionId, HttpServletRequest request) {
        Long userId = AuthSessionSupport.requireUserId(request);
        AuthSessionSupport.requireActiveRole(request, UserRole.SEEKER);
        agentChatService.deleteSession(userId, sessionId);
        return ApiResponse.ok(null);
    }

    public record ChatRequest(String message) {}
}