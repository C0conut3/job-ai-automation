package com.jobai.automation.user.web.dto;

import com.jobai.automation.user.UserRole;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record LoginRequest(
        @NotBlank String username,
        @NotBlank String password,
        /** 本次登录使用的身份，必须与账号已拥有的角色一致。 */
        @NotNull UserRole activeRole) {}
