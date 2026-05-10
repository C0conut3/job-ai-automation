package com.jobai.automation.chat.repository;

import com.jobai.automation.chat.domain.ChatMessage;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {
    List<ChatMessage> findBySessionIdOrderByCreatedAtAsc(Long sessionId);
    List<ChatMessage> findBySessionIdAndSenderUserIdNotAndIsReadFalse(Long sessionId, Long currentUserId);
    int countBySessionIdAndSenderUserIdNotAndIsReadFalse(Long sessionId, Long currentUserId);
}