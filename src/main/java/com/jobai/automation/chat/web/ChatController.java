package com.jobai.automation.chat.web;

import com.jobai.automation.chat.service.ChatService;
import com.jobai.automation.chat.web.dto.ChatMessageDto;
import com.jobai.automation.chat.web.dto.ChatSessionDto;
import com.jobai.automation.chat.web.dto.SendMessageRequest;
import com.jobai.automation.common.api.ApiResponse;
import com.jobai.automation.user.UserRole;
import com.jobai.automation.user.support.AuthSessionSupport;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/chat")
public class ChatController {

    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    @PostMapping("/invite")
    public ApiResponse<Void> inviteSeeker(@RequestParam Long seekerUserId, @RequestParam Long jobId, HttpServletRequest request) {
        Long recruiterUserId = AuthSessionSupport.requireUserId(request);
        AuthSessionSupport.requireActiveRole(request, UserRole.RECRUITER);
        chatService.sendInvitation(recruiterUserId, seekerUserId, jobId);
        return ApiResponse.ok(null);
    }

    @GetMapping("/sessions")
    public ApiResponse<List<ChatSessionDto>> listSessions(HttpServletRequest request) {
        Long userId = AuthSessionSupport.requireUserId(request);
        UserRole role = AuthSessionSupport.requireActiveRole(request, UserRole.SEEKER, UserRole.RECRUITER);
        return ApiResponse.ok(chatService.listMySessions(userId, role.name()));
    }

    @GetMapping("/unread-count")
    public ApiResponse<Integer> getUnreadCount(HttpServletRequest request) {
        Long userId = AuthSessionSupport.requireUserId(request);
        UserRole role = AuthSessionSupport.requireActiveRole(request, UserRole.SEEKER, UserRole.RECRUITER);
        return ApiResponse.ok(chatService.getTotalUnreadCount(userId, role.name()));
    }

    @PostMapping("/sessions/{sessionId}/read")
    public ApiResponse<Void> markAsRead(@PathVariable Long sessionId, HttpServletRequest request) {
        Long userId = AuthSessionSupport.requireUserId(request);
        AuthSessionSupport.requireActiveRole(request, UserRole.SEEKER, UserRole.RECRUITER);
        chatService.markSessionAsRead(userId, sessionId);
        return ApiResponse.ok(null);
    }

    @GetMapping("/sessions/{sessionId}/messages")
    public ApiResponse<List<ChatMessageDto>> listMessages(@PathVariable Long sessionId, HttpServletRequest request) {
        Long userId = AuthSessionSupport.requireUserId(request);
        AuthSessionSupport.requireActiveRole(request, UserRole.SEEKER, UserRole.RECRUITER);
        return ApiResponse.ok(chatService.listMessages(userId, sessionId));
    }

    @PostMapping("/sessions/{sessionId}/messages")
    public ApiResponse<ChatMessageDto> sendMessage(
            @PathVariable Long sessionId,
            @Valid @RequestBody SendMessageRequest body,
            HttpServletRequest request) {
        Long userId = AuthSessionSupport.requireUserId(request);
        AuthSessionSupport.requireActiveRole(request, UserRole.SEEKER, UserRole.RECRUITER);
        return ApiResponse.ok(chatService.sendMessage(userId, sessionId, body));
    }
}