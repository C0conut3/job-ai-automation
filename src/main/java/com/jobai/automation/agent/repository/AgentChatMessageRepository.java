package com.jobai.automation.agent.repository;

import com.jobai.automation.agent.domain.AgentChatMessage;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AgentChatMessageRepository extends JpaRepository<AgentChatMessage, Long> {
    List<AgentChatMessage> findBySessionIdOrderByCreatedAtAsc(Long sessionId);
    void deleteBySessionId(Long sessionId);
}