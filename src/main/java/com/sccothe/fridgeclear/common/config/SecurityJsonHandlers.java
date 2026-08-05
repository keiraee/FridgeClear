package com.sccothe.fridgeclear.common.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sccothe.fridgeclear.common.api.ApiResponse;
import com.sccothe.fridgeclear.common.api.RequestIdFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * 将 Spring Security 鉴权失败统一为 JSON，避免返回 Whitelabel HTML 页面。
 */
@Component
public class SecurityJsonHandlers {
    private final ObjectMapper objectMapper;

    public SecurityJsonHandlers(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public AuthenticationEntryPoint authenticationEntryPoint() {
        return (request, response, authException) ->
                write(response, request, HttpServletResponse.SC_UNAUTHORIZED, "UNAUTHORIZED", "请先登录");
    }

    public AccessDeniedHandler accessDeniedHandler() {
        return (request, response, accessDeniedException) -> {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth == null || auth instanceof AnonymousAuthenticationToken) {
                write(response, request, HttpServletResponse.SC_UNAUTHORIZED, "UNAUTHORIZED", "请先登录");
                return;
            }
            write(response, request, HttpServletResponse.SC_FORBIDDEN, "FORBIDDEN", "无权访问该资源");
        };
    }

    private void write(HttpServletResponse response, HttpServletRequest request,
                       int status, String code, String message) throws IOException {
        response.setStatus(status);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        Object requestIdValue = request.getAttribute(RequestIdFilter.REQUEST_ID_ATTRIBUTE);
        String requestId = requestIdValue == null ? "req_security" : requestIdValue.toString();
        objectMapper.writeValue(response.getWriter(), new ApiResponse<>(code, message, null, requestId));
    }
}
