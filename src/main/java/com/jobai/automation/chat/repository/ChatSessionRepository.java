package com.jobai.automation.chat.repository;

import com.jobai.automation.chat.domain.ChatSession;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ChatSessionRepository extends JpaRepository<ChatSession, Long> {
    List<ChatSession> findBySeekerUserIdOrderByUpdatedAtDesc(Long seekerUserId);
    List<ChatSession> findByRecruiterUserIdOrderByUpdatedAtDesc(Long recruiterUserId);
    Optional<ChatSession> findByJobIdAndSeekerUserId(Long jobId, Long seekerUserId);
}