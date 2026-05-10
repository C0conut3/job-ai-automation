package com.jobai.automation.chat.service;

import com.jobai.automation.applicationrecord.domain.JobApplicationRecord;
import com.jobai.automation.applicationrecord.domain.JobApplicationStatus;
import com.jobai.automation.applicationrecord.repository.JobApplicationRecordRepository;
import com.jobai.automation.chat.domain.ChatMessage;
import com.jobai.automation.chat.domain.ChatSession;
import com.jobai.automation.chat.repository.ChatMessageRepository;
import com.jobai.automation.chat.repository.ChatSessionRepository;
import com.jobai.automation.chat.web.dto.ChatMessageDto;
import com.jobai.automation.chat.web.dto.ChatSessionDto;
import com.jobai.automation.chat.web.dto.SendMessageRequest;
import com.jobai.automation.common.exception.ApiException;
import com.jobai.automation.job.domain.Job;
import com.jobai.automation.job.repository.JobRepository;
import com.jobai.automation.user.domain.User;
import com.jobai.automation.user.repository.UserRepository;
import java.time.Instant;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ChatService {

    private final ChatSessionRepository sessionRepository;
    private final ChatMessageRepository messageRepository;
    private final JobRepository jobRepository;
    private final UserRepository userRepository;
    private final JobApplicationRecordRepository applicationRecordRepository;

    public ChatService(ChatSessionRepository sessionRepository, ChatMessageRepository messageRepository, JobRepository jobRepository, UserRepository userRepository, JobApplicationRecordRepository applicationRecordRepository) {
        this.sessionRepository = sessionRepository;
        this.messageRepository = messageRepository;
        this.jobRepository = jobRepository;
        this.userRepository = userRepository;
        this.applicationRecordRepository = applicationRecordRepository;
    }

    @Transactional
    public void sendInvitation(Long recruiterUserId, Long seekerUserId, Long jobId) {
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "职位不存在"));
        
        if (!job.getRecruiterUserId().equals(recruiterUserId)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "无权操作该职位");
        }

        ChatSession session = sessionRepository.findByJobIdAndSeekerUserId(jobId, seekerUserId)
                .orElseGet(() -> {
                    ChatSession newSession = new ChatSession();
                    newSession.setJobId(jobId);
                    newSession.setSeekerUserId(seekerUserId);
                    newSession.setRecruiterUserId(recruiterUserId);
                    return sessionRepository.save(newSession);
                });

        ChatMessage msg = new ChatMessage();
        msg.setSessionId(session.getId());
        msg.setSenderUserId(recruiterUserId);
        msg.setMsgType("JOB_INVITE");
        
        // 构造 JSON 格式的卡片数据
        String salaryStr = "面议";
        if (job.getSalaryMin() != null && job.getSalaryMax() != null) {
            salaryStr = (job.getSalaryMin() / 1000) + "k - " + (job.getSalaryMax() / 1000) + "k";
        }
        
        String jsonContent = String.format(
            "{\"jobId\": %d, \"title\": \"%s\", \"company\": \"%s\", \"salary\": \"%s\", \"city\": \"%s\"}",
            job.getId(),
            job.getTitle().replace("\"", "\\\""),
            job.getCompanyName().replace("\"", "\\\""),
            salaryStr,
            (job.getCity() == null ? "全国" : job.getCity().replace("\"", "\\\""))
        );
        
        msg.setContent(jsonContent);
        messageRepository.save(msg);

        session.setUpdatedAt(Instant.now());
        sessionRepository.save(session);
        
        // 记录邀请投递状态
        if (!applicationRecordRepository.existsByJob_IdAndSeekerUserId(jobId, seekerUserId)) {
            JobApplicationRecord record = new JobApplicationRecord();
            record.setJob(job);
            record.setSeekerUserId(seekerUserId);
            record.setStatus(JobApplicationStatus.INVITED);
            record.setAppliedAt(Instant.now());
            applicationRecordRepository.save(record);
        }
    }

    @Transactional
    public void initiateApplicationChat(Long seekerUserId, Job job, String resumeContent) {
        ChatSession session = sessionRepository.findByJobIdAndSeekerUserId(job.getId(), seekerUserId)
                .orElseGet(() -> {
                    ChatSession newSession = new ChatSession();
                    newSession.setJobId(job.getId());
                    newSession.setSeekerUserId(seekerUserId);
                    newSession.setRecruiterUserId(job.getRecruiterUserId());
                    return sessionRepository.save(newSession);
                });

        // 自动发送简历
        if (resumeContent != null && !resumeContent.isBlank()) {
            ChatMessage msg = new ChatMessage();
            msg.setSessionId(session.getId());
            msg.setSenderUserId(seekerUserId);
            msg.setMsgType("RESUME");
            msg.setContent("【自动投递简历】\n" + resumeContent);
            messageRepository.save(msg);

            session.setUpdatedAt(Instant.now());
            sessionRepository.save(session);
        }
    }

    @Transactional(readOnly = true)
    public List<ChatSessionDto> listMySessions(Long userId, String role) {
        List<ChatSession> sessions;
        if ("SEEKER".equals(role)) {
            sessions = sessionRepository.findBySeekerUserIdOrderByUpdatedAtDesc(userId);
        } else if ("RECRUITER".equals(role)) {
            sessions = sessionRepository.findByRecruiterUserIdOrderByUpdatedAtDesc(userId);
        } else {
            throw new ApiException(HttpStatus.FORBIDDEN, "不支持的角色");
        }
        return sessions.stream().map(s -> toSessionDto(s, userId)).toList();
    }

    @Transactional(readOnly = true)
    public int getTotalUnreadCount(Long userId, String role) {
        List<ChatSession> sessions;
        if ("SEEKER".equals(role)) {
            sessions = sessionRepository.findBySeekerUserIdOrderByUpdatedAtDesc(userId);
        } else if ("RECRUITER".equals(role)) {
            sessions = sessionRepository.findByRecruiterUserIdOrderByUpdatedAtDesc(userId);
        } else {
            return 0;
        }
        return sessions.stream()
                .mapToInt(s -> messageRepository.countBySessionIdAndSenderUserIdNotAndIsReadFalse(s.getId(), userId))
                .sum();
    }

    @Transactional
    public void markSessionAsRead(Long userId, Long sessionId) {
        ChatSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "会话不存在"));
        
        if (!session.getSeekerUserId().equals(userId) && !session.getRecruiterUserId().equals(userId)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "无权访问此会话");
        }
        
        List<ChatMessage> unreadMessages = messageRepository.findBySessionIdAndSenderUserIdNotAndIsReadFalse(sessionId, userId);
        for (ChatMessage msg : unreadMessages) {
            msg.setIsRead(true);
        }
        messageRepository.saveAll(unreadMessages);
    }

    @Transactional(readOnly = true)
    public List<ChatMessageDto> listMessages(Long userId, Long sessionId) {
        ChatSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "会话不存在"));
        
        if (!session.getSeekerUserId().equals(userId) && !session.getRecruiterUserId().equals(userId)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "无权访问此会话");
        }
        
        return messageRepository.findBySessionIdOrderByCreatedAtAsc(sessionId).stream()
                .map(this::toMessageDto).toList();
    }

    @Transactional
    public ChatMessageDto sendMessage(Long userId, Long sessionId, SendMessageRequest request) {
        ChatSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "会话不存在"));
        
        if (!session.getSeekerUserId().equals(userId) && !session.getRecruiterUserId().equals(userId)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "无权访问此会话");
        }

        ChatMessage msg = new ChatMessage();
        msg.setSessionId(sessionId);
        msg.setSenderUserId(userId);
        msg.setMsgType(request.msgType());
        msg.setContent(request.content());
        messageRepository.save(msg);

        session.setUpdatedAt(Instant.now());
        sessionRepository.save(session);

        return toMessageDto(msg);
    }

    private ChatSessionDto toSessionDto(ChatSession s, Long currentUserId) {
        String jobTitle = jobRepository.findById(s.getJobId()).map(Job::getTitle).orElse("未知职位");
        String seekerName = userRepository.findById(s.getSeekerUserId()).map(User::getNickname).orElse("未知求职者");
        String recruiterName = userRepository.findById(s.getRecruiterUserId()).map(User::getNickname).orElse("未知招聘者");
        int unreadCount = messageRepository.countBySessionIdAndSenderUserIdNotAndIsReadFalse(s.getId(), currentUserId);

        return new ChatSessionDto(
                s.getId(), s.getJobId(), jobTitle,
                s.getSeekerUserId(), seekerName,
                s.getRecruiterUserId(), recruiterName,
                s.getUpdatedAt(), unreadCount);
    }

    private ChatMessageDto toMessageDto(ChatMessage m) {
        String senderName = userRepository.findById(m.getSenderUserId()).map(User::getNickname).orElse("未知");
        return new ChatMessageDto(
                m.getId(), m.getSessionId(),
                m.getSenderUserId(), senderName,
                m.getMsgType(), m.getContent(), m.getCreatedAt());
    }
}