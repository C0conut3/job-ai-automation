package com.jobai.automation.user.web.dto;

import java.time.Instant;
import java.util.List;

public record UserProfileResponse(
        Long id,
        String username,
        String email,
        String nickname,
        Instant createdAt,
        String activeRole,
        List<String> roles) {}
