package com.jobai.automation.user.web;

import com.jobai.automation.common.api.ApiResponse;
import com.jobai.automation.user.service.AuthService;
import com.jobai.automation.user.web.dto.AccountDeleteRequest;
import com.jobai.automation.user.web.dto.LoginRequest;
import com.jobai.automation.user.web.dto.RegisterRequest;
import com.jobai.automation.user.web.dto.UserProfileResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<UserProfileResponse>> register(
            HttpServletRequest httpRequest, @Valid @RequestBody RegisterRequest request) {
        UserProfileResponse profile = authService.register(httpRequest, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(profile));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<UserProfileResponse>> login(
            HttpServletRequest httpRequest, @Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(authService.login(httpRequest, request)));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(HttpServletRequest httpRequest) {
        authService.logout(httpRequest);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserProfileResponse>> me(HttpServletRequest httpRequest) {
        return ResponseEntity.ok(ApiResponse.ok(authService.currentUser(httpRequest)));
    }

    @PostMapping("/account/delete")
    public ResponseEntity<ApiResponse<Void>> deleteAccount(
            HttpServletRequest httpRequest, @Valid @RequestBody AccountDeleteRequest request) {
        authService.deleteAccount(httpRequest, request);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }
}
