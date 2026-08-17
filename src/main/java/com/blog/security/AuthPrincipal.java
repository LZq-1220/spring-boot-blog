package com.blog.security;

import com.blog.exception.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticatedPrincipal;

/**
 * 认证主体里携带的用户信息。
 * 原先把 userId 塞进 Authentication.getDetails() 再强转 (Long)，
 * 一旦其它过滤器覆写 details 就会 ClassCastException。改为专用的 principal 类型。
 *
 * 实现 AuthenticatedPrincipal 让 Authentication.getName() 明确返回 username，
 * 而不是依赖 toString() 兜底。
 */
public record AuthPrincipal(Long userId, String username, String role) implements AuthenticatedPrincipal {

    @Override
    public String getName() {
        return username;
    }

    public static AuthPrincipal from(Authentication auth) {
        if (auth == null || !(auth.getPrincipal() instanceof AuthPrincipal principal)) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "未登录");
        }
        return principal;
    }

    public static Long userIdOf(Authentication auth) {
        return from(auth).userId();
    }
}
