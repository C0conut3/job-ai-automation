package com.jobai.automation.user.service;

import com.jobai.automation.user.web.dto.AccountDeleteRequest;
import com.jobai.automation.user.web.dto.LoginRequest;
import com.jobai.automation.user.web.dto.RegisterRequest;
import com.jobai.automation.user.web.dto.UserProfileResponse;
import jakarta.servlet.http.HttpServletRequest;

public interface AuthService {

    UserProfileResponse register(HttpServletRequest httpRequest, RegisterRequest request);

    UserProfileResponse login(HttpServletRequest httpRequest, LoginRequest request);

    void logout(HttpServletRequest httpRequest);

    UserProfileResponse currentUser(HttpServletRequest httpRequest);

    void deleteAccount(HttpServletRequest httpRequest, AccountDeleteRequest request);
}
