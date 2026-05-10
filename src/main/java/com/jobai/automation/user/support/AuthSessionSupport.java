package com.jobai.automation.user.support;

import com.jobai.automation.common.exception.ApiException;
import com.jobai.automation.user.AuthConstants;
import com.jobai.automation.user.UserRole;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.util.Arrays;
import org.springframework.http.HttpStatus;

public final class AuthSessionSupport {

    private AuthSessionSupport() {}

    public static Long getUserId(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
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

    public static UserRole getActiveRole(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
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

    public static Long requireUserId(HttpServletRequest request) {
        Long id = getUserId(request);
        if (id == null) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "未登录");
        }
        return id;
    }

    public static UserRole requireActiveRole(HttpServletRequest request, UserRole... allowed) {
        UserRole role = getActiveRole(request);
        if (role == null) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "未登录或会话缺少身份，请重新登录");
        }
        if (Arrays.stream(allowed).noneMatch(r -> r == role)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "当前登录身份无权执行此操作");
        }
        return role;
    }
}
