package com.sccothe.fridgeclear.auth.api;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public final class AuthDtos {
    private AuthDtos() {}

    @Schema(name = "RegisterRequest", description = "用户注册请求")
    public record RegisterRequest(
            @NotBlank @Email @Size(max = 190) String email,
            @NotBlank @Size(min = 8, max = 72) String password,
            @NotBlank @Size(max = 64) String nickname
    ) {}

    @Schema(name = "LoginRequest", description = "用户登录请求")
    public record LoginRequest(
            @NotBlank @Email @Size(max = 190) String email,
            @NotBlank String password
    ) {}

    public record UserResponse(Long id, String email, String nickname, String role) {}
    public record AuthResponse(UserResponse user, boolean authenticated, String accessToken,
                               String tokenType, long expiresIn) {}
}
