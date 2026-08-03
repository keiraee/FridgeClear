package com.sccothe.fridgeclear.auth.service;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import com.sccothe.fridgeclear.common.api.AuthenticationFailedException;

public final class CurrentUser {
    private CurrentUser() {}

    public static Long id() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || !(authentication.getPrincipal() instanceof String)
                || "anonymousUser".equals(authentication.getPrincipal())) {
            throw new AuthenticationFailedException("请先登录");
        }
        try {
            return Long.valueOf(authentication.getPrincipal().toString());
        } catch (NumberFormatException exception) {
            throw new AuthenticationFailedException("登录信息无效，请重新登录");
        }
    }
}
