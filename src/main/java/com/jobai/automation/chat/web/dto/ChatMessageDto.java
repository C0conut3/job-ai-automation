package com.jobai.automation.chat.web.dto;

import java.time.Instant;

public record ChatMessageDto(
        Long id,
        Long sessionId,
        Long senderUserId,
        String senderName,
        String msgType,
        String content,
        Instant createdAt) {}