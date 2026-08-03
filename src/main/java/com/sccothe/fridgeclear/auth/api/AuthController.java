package com.sccothe.fridgeclear.auth.api;

import com.sccothe.fridgeclear.auth.service.AuthService;
import com.sccothe.fridgeclear.auth.service.CurrentUser;
import com.sccothe.fridgeclear.common.api.ApiResponse;
import com.sccothe.fridgeclear.common.api.RequestIdFilter;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "用户认证", description = "用户注册和登录")
public class AuthController {
    private final AuthService service;
    public AuthController(AuthService service) { this.service = service; }

    @PostMapping("/register")
    @Operation(summary = "用户注册")
    public ApiResponse<AuthDtos.AuthResponse> register(@Valid @RequestBody AuthDtos.RegisterRequest body, HttpServletRequest request) {
        return success(service.register(body), request);
    }

    @PostMapping("/login")
    @Operation(summary = "用户登录")
    public ApiResponse<AuthDtos.AuthResponse> login(@Valid @RequestBody AuthDtos.LoginRequest body, HttpServletRequest request) {
        return success(service.login(body), request);
    }

    @GetMapping("/me")
    @Operation(summary = "获取当前用户")
    @SecurityRequirement(name = "Authorization")
    public ApiResponse<AuthDtos.UserResponse> me(HttpServletRequest request) {
        return success(service.me(CurrentUser.id()), request);
    }

    private <T> ApiResponse<T> success(T data, HttpServletRequest request) {
        Object value = request.getAttribute(RequestIdFilter.REQUEST_ID_ATTRIBUTE);
        String requestId = value == null ? "req_" + UUID.randomUUID() : value.toString();
        return ApiResponse.success(data, requestId);
    }
}
