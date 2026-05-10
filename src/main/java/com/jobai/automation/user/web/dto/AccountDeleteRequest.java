package com.jobai.automation.user.web.dto;

import jakarta.validation.constraints.NotBlank;

public record AccountDeleteRequest(@NotBlank String password) {}
