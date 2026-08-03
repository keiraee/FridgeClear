package com.sccothe.fridgeclear.auth.service;

import com.sccothe.fridgeclear.auth.api.AuthDtos;
import com.sccothe.fridgeclear.auth.domain.UserAccount;
import com.sccothe.fridgeclear.auth.repository.UserAccountRepository;
import com.sccothe.fridgeclear.common.api.AuthenticationFailedException;
import com.sccothe.fridgeclear.common.api.ResourceConflictException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class AuthService {
    private final UserAccountRepository repository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(UserAccountRepository repository, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    public AuthDtos.AuthResponse register(AuthDtos.RegisterRequest request) {
        String email = normalizeEmail(request.email());
        if (repository.existsByEmail(email)) throw new ResourceConflictException("邮箱已注册");
        UserAccount user = new UserAccount();
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setNickname(request.nickname().trim());
        user.setRole(UserAccount.UserRole.USER);
        user.setStatus(UserAccount.UserStatus.ACTIVE);
        user = repository.save(user);
        return authResponse(user);
    }

    @Transactional(readOnly = true)
    public AuthDtos.AuthResponse login(AuthDtos.LoginRequest request) {
        UserAccount user = repository.findByEmail(normalizeEmail(request.email()))
                .orElseThrow(() -> new AuthenticationFailedException("邮箱或密码错误"));
        if (user.getStatus() != UserAccount.UserStatus.ACTIVE || !passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new AuthenticationFailedException("邮箱或密码错误");
        }
        return authResponse(user);
    }

    @Transactional(readOnly = true)
    public AuthDtos.UserResponse me(Long userId) {
        return repository.findById(userId).map(this::toResponse)
                .orElseThrow(() -> new AuthenticationFailedException("用户不存在"));
    }

    private AuthDtos.AuthResponse authResponse(UserAccount user) {
        return new AuthDtos.AuthResponse(toResponse(user), true, jwtService.generate(user), "Bearer", jwtService.expirationSeconds());
    }

    private AuthDtos.UserResponse toResponse(UserAccount user) {
        return new AuthDtos.UserResponse(user.getId(), user.getEmail(), user.getNickname(), user.getRole().name());
    }

    private String normalizeEmail(String email) { return email.trim().toLowerCase(java.util.Locale.ROOT); }
}
