package com.jobai.automation.user.web.dto;

import com.jobai.automation.user.UserRole;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank @Size(min = 3, max = 64) String username,
        @NotBlank @Size(min = 6, max = 72) String password,
        @Size(max = 128) String email,
        @Size(max = 64) String nickname,
        /** 注册时只能选择求职方或招聘方；管理员由系统分配。 */
        @NotNull UserRole primaryRole) {}
