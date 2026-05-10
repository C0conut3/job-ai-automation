package com.jobai.automation.user.service.impl;

import com.jobai.automation.applicationrecord.repository.JobApplicationRecordRepository;
import com.jobai.automation.common.exception.ApiException;
import com.jobai.automation.job.domain.Job;
import com.jobai.automation.job.repository.JobRepository;
import com.jobai.automation.user.AuthConstants;
import com.jobai.automation.user.UserRole;
import com.jobai.automation.user.domain.User;
import com.jobai.automation.user.domain.UserRoleEntity;
import com.jobai.automation.user.repository.UserRepository;
import com.jobai.automation.user.repository.UserRoleRepository;
import com.jobai.automation.user.service.AuthService;
import com.jobai.automation.user.web.dto.AccountDeleteRequest;
import com.jobai.automation.user.web.dto.LoginRequest;
import com.jobai.automation.user.web.dto.RegisterRequest;
import com.jobai.automation.user.web.dto.UserProfileResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.util.List;
import java.util.regex.Pattern;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthServiceImpl implements AuthService {

    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[\\w.+-]+@[\\w.-]+\\.[A-Za-z]{2,}$");

    private final UserRepository userRepository;
    private final UserRoleRepository userRoleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JobRepository jobRepository;
    private final JobApplicationRecordRepository jobApplicationRecordRepository;

    public AuthServiceImpl(
            UserRepository userRepository,
            UserRoleRepository userRoleRepository,
            PasswordEncoder passwordEncoder,
            JobRepository jobRepository,
            JobApplicationRecordRepository jobApplicationRecordRepository) {
        this.userRepository = userRepository;
        this.userRoleRepository = userRoleRepository;
        this.passwordEncoder = passwordEncoder;
        this.jobRepository = jobRepository;
        this.jobApplicationRecordRepository = jobApplicationRecordRepository;
    }

    @Override
    @Transactional
    public UserProfileResponse register(HttpServletRequest httpRequest, RegisterRequest request) {
        if (request.primaryRole() == UserRole.ADMIN) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "不能自助注册为管理员");
        }
        String username = normalizeUsername(request.username());
        String email = normalizeOptional(request.email());
        String nickname = normalizeOptional(request.nickname());

        if (email != null && !EMAIL_PATTERN.matcher(email).matches()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "邮箱格式不正确");
        }
        if (userRepository.existsByUsername(username)) {
            throw new ApiException(HttpStatus.CONFLICT, "用户名已被占用");
        }
        if (email != null && userRepository.existsByEmail(email)) {
            throw new ApiException(HttpStatus.CONFLICT, "邮箱已被占用");
        }

        User user = new User();
        user.setUsername(username);
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setEmail(email);
        user.setNickname(nickname);
        userRepository.save(user);

        userRoleRepository.save(new UserRoleEntity(user.getId(), request.primaryRole()));

        rotateSession(httpRequest);
        HttpSession session = httpRequest.getSession(true);
        session.setAttribute(AuthConstants.SESSION_USER_ID, user.getId());
        session.setAttribute(AuthConstants.SESSION_ACTIVE_ROLE, request.primaryRole());

        return buildProfile(user, request.primaryRole());
    }

    @Override
    @Transactional(readOnly = true)
    public UserProfileResponse login(HttpServletRequest httpRequest, LoginRequest request) {
        String username = normalizeUsername(request.username());
        User user = userRepository.findByUsername(username).orElseThrow(AuthServiceImpl::unauthorized);

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw unauthorized();
        }
        if (!userRoleRepository.existsByUserIdAndRole(user.getId(), request.activeRole())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "该账号没有所选身份，请检查角色或联系管理员");
        }

        rotateSession(httpRequest);
        HttpSession session = httpRequest.getSession(true);
        session.setAttribute(AuthConstants.SESSION_USER_ID, user.getId());
        session.setAttribute(AuthConstants.SESSION_ACTIVE_ROLE, request.activeRole());

        return buildProfile(user, request.activeRole());
    }

    @Override
    public void logout(HttpServletRequest httpRequest) {
        HttpSession session = httpRequest.getSession(false);
        if (session != null) {
            session.invalidate();
        }
    }

    @Override
    @Transactional(readOnly = true)
    public UserProfileResponse currentUser(HttpServletRequest httpRequest) {
        Long userId = currentUserIdOrNull(httpRequest);
        if (userId == null) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "未登录");
        }
        User user = userRepository.findById(userId).orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "登录已失效，请重新登录"));
        UserRole active = readActiveRoleFromSession(httpRequest);
        if (active == null) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "会话缺少身份，请重新登录");
        }
        return buildProfile(user, active);
    }

    @Override
    @Transactional
    public void deleteAccount(HttpServletRequest httpRequest, AccountDeleteRequest request) {
        Long userId = currentUserIdOrNull(httpRequest);
        if (userId == null) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "未登录");
        }
        User user = userRepository.findById(userId).orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "登录已失效，请重新登录"));

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "密码不正确，无法注销账号");
        }

        List<Job> ownJobs = jobRepository.findAllByRecruiterUserId(userId);
        for (Job j : ownJobs) {
            jobApplicationRecordRepository.deleteByJob_Id(j.getId());
        }
        jobRepository.deleteAll(ownJobs);
        jobApplicationRecordRepository.deleteBySeekerUserId(userId);
        userRoleRepository.deleteByUserId(userId);
        userRepository.delete(user);
        logout(httpRequest);
    }

    private UserProfileResponse buildProfile(User user, UserRole activeRole) {
        List<String> roleNames =
                userRoleRepository.findByUserId(user.getId()).stream().map(ur -> ur.getRole().name()).toList();
        return new UserProfileResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getNickname(),
                user.getCreatedAt(),
                activeRole.name(),
                roleNames);
    }

    private static ApiException unauthorized() {
        return new ApiException(HttpStatus.UNAUTHORIZED, "用户名或密码错误");
    }

    private static void rotateSession(HttpServletRequest httpRequest) {
        HttpSession old = httpRequest.getSession(false);
        if (old != null) {
            old.invalidate();
        }
    }

    private Long currentUserIdOrNull(HttpServletRequest httpRequest) {
        HttpSession session = httpRequest.getSession(false);
        if (session == null) {
            return null;
        }
        Object v = session.getAttribute(AuthConstants.SESSION_USER_ID);
        if (v instanceof Long l) {
            return l;
        }
        if (v instanceof Number n) {
            return n.longValue();
        }
        return null;
    }

    private static UserRole readActiveRoleFromSession(HttpServletRequest httpRequest) {
        HttpSession session = httpRequest.getSession(false);
        if (session == null) {
            return null;
        }
        Object v = session.getAttribute(AuthConstants.SESSION_ACTIVE_ROLE);
        if (v instanceof UserRole ur) {
            return ur;
        }
        if (v instanceof String s) {
            try {
                return UserRole.valueOf(s);
            } catch (IllegalArgumentException ignored) {
                return null;
            }
        }
        return null;
    }

    private static String normalizeUsername(String raw) {
        if (raw == null) {
            return "";
        }
        return raw.trim();
    }

    private static String normalizeOptional(String raw) {
        if (raw == null) {
            return null;
        }
        String t = raw.trim();
        return t.isEmpty() ? null : t;
    }
}
