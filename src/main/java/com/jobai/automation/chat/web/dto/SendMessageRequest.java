package com.jobai.automation.chat.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record SendMessageRequest(
        @NotBlank String msgType,
        @NotBlank String content) {}