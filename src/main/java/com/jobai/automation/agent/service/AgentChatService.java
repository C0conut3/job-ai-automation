package com.jobai.automation.agent.service;

import com.jobai.automation.agent.dto.AgentResponse;
import java.util.List;

public interface AgentChatService {

    List<SessionSummary> getUserSessions(Long userId);

    SessionDetail getSessionDetail(Long userId, Long sessionId);

    AgentResponse createSessionAndQuery(Long userId, String message);

    AgentResponse query(Long userId, Long sessionId, String message);

    void deleteSession(Long userId, Long sessionId);

    record SessionSummary(Long id, String title, java.time.Instant createdAt) {}

    record ChatMessageDto(String role, String content, List<?> jobs) {}

    record SessionDetail(Long id, String title, List<ChatMessageDto> messages, java.time.Instant createdAt) {}
}