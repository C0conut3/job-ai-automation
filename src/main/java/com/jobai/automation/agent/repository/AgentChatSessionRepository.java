package com.jobai.automation.agent.repository;

import com.jobai.automation.agent.domain.AgentChatSession;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AgentChatSessionRepository extends JpaRepository<AgentChatSession, Long> {
    List<AgentChatSession> findByUserIdOrderByUpdatedAtDesc(Long userId);
    Optional<AgentChatSession> findByIdAndUserId(Long id, Long userId);
    void deleteByIdAndUserId(Long id, Long userId);
}