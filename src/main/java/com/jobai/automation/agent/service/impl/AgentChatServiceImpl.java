package com.jobai.automation.agent.service.impl;

import com.jobai.automation.agent.domain.AgentChatMessage;
import com.jobai.automation.agent.domain.AgentChatSession;
import com.jobai.automation.agent.dto.AgentRequest;
import com.jobai.automation.agent.dto.AgentResponse;
import com.jobai.automation.agent.repository.AgentChatMessageRepository;
import com.jobai.automation.agent.repository.AgentChatSessionRepository;
import com.jobai.automation.agent.service.AgentChatService;
import com.jobai.automation.agent.service.AgentChatService.ChatMessageDto;
import com.jobai.automation.agent.service.UserAgentService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AgentChatServiceImpl implements AgentChatService {

    private final AgentChatSessionRepository sessionRepository;
    private final AgentChatMessageRepository messageRepository;
    private final UserAgentService userAgentService;
    private final ObjectMapper objectMapper;

    public AgentChatServiceImpl(AgentChatSessionRepository sessionRepository,
                               AgentChatMessageRepository messageRepository,
                               UserAgentService userAgentService,
                               ObjectMapper objectMapper) {
        this.sessionRepository = sessionRepository;
        this.messageRepository = messageRepository;
        this.userAgentService = userAgentService;
        this.objectMapper = objectMapper;
    }

    @Override
    public List<SessionSummary> getUserSessions(Long userId) {
        return sessionRepository.findByUserIdOrderByUpdatedAtDesc(userId)
                .stream()
                .map(s -> new SessionSummary(s.getId(), s.getTitle(), s.getCreatedAt()))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public SessionDetail getSessionDetail(Long userId, Long sessionId) {
        AgentChatSession session = sessionRepository.findByIdAndUserId(sessionId, userId)
                .orElseThrow(() -> new IllegalArgumentException("Session not found"));

        List<ChatMessageDto> messages = messageRepository.findBySessionIdOrderByCreatedAtAsc(sessionId)
                .stream()
                .map(this::toChatMessageDto)
                .collect(Collectors.toList());

        return new SessionDetail(session.getId(), session.getTitle(), messages, session.getCreatedAt());
    }

    @Override
    @Transactional
    public AgentResponse createSessionAndQuery(Long userId, String message) {
        AgentChatSession session = new AgentChatSession();
        session.setUserId(userId);
        session.setTitle(truncateMessage(message));
        session = sessionRepository.save(session);

        return query(userId, session.getId(), message);
    }

    @Override
    @Transactional
    public AgentResponse query(Long userId, Long sessionId, String message) {
        AgentChatSession session = sessionRepository.findByIdAndUserId(sessionId, userId)
                .orElseThrow(() -> new IllegalArgumentException("Session not found"));

        AgentRequest request = new AgentRequest(String.valueOf(userId), null, message, null, String.valueOf(sessionId));
        AgentResponse response = userAgentService.handle(request);

        String assistantContent = response.conversation().isEmpty() ? "" : response.conversation().get(0).content();
        saveMessage(sessionId, "user", message, null);
        saveMessage(sessionId, "assistant", assistantContent, serializeJobs(response.jobs()));

        session.setTitle(truncateMessage(message));
        sessionRepository.save(session);

        return response;
    }

    @Override
    @Transactional
    public void deleteSession(Long userId, Long sessionId) {
        if (sessionRepository.findByIdAndUserId(sessionId, userId).isEmpty()) {
            throw new IllegalArgumentException("Session not found");
        }
        messageRepository.deleteBySessionId(sessionId);
        sessionRepository.deleteByIdAndUserId(sessionId, userId);
    }

    private void saveMessage(Long sessionId, String role, String content, String jobsJson) {
        AgentChatMessage message = new AgentChatMessage();
        message.setSessionId(sessionId);
        message.setRole(role);
        message.setContent(content);
        message.setJobsJson(jobsJson);
        messageRepository.save(message);
    }

    private ChatMessageDto toChatMessageDto(AgentChatMessage message) {
        return new ChatMessageDto(
                message.getRole(),
                message.getContent(),
                deserializeJobs(message.getJobsJson())
        );
    }

    private String serializeJobs(List<?> jobs) {
        if (jobs == null || jobs.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(jobs);
        } catch (JsonProcessingException e) {
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private List<?> deserializeJobs(String jobsJson) {
        if (jobsJson == null || jobsJson.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readValue(jobsJson, List.class);
        } catch (JsonProcessingException e) {
            return null;
        }
    }

    private String truncateMessage(String message) {
        if (message == null) {
            return "新对话";
        }
        return message.length() > 20 ? message.substring(0, 20) + "..." : message;
    }
}