package com.sccothe.fridgeclear.auth.service;

import com.sccothe.fridgeclear.auth.domain.UserAccount;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;

@Service
public class JwtService {
    private final SecretKey signingKey;
    private final long expirationSeconds;

    public JwtService(@Value("${fridgeclear.auth.jwt-secret:}") String encodedSecret,
                      @Value("${fridgeclear.auth.jwt-expiration-seconds:7200}") long expirationSeconds) {
        try {
            byte[] secret = Base64.getDecoder().decode(encodedSecret);
            if (secret.length < 32) throw new IllegalArgumentException();
            signingKey = Keys.hmacShaKeyFor(secret);
        } catch (Exception exception) {
            throw new IllegalStateException("AUTH_JWT_SECRET 必须是 Base64 编码的至少 32 字节密钥", exception);
        }
        this.expirationSeconds = expirationSeconds;
    }

    public String generate(UserAccount user) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(user.getId().toString())
                .claim("email", user.getEmail())
                .claim("role", user.getRole().name())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(expirationSeconds)))
                .signWith(signingKey)
                .compact();
    }

    public Claims parse(String token) {
        return Jwts.parser().verifyWith(signingKey).build().parseSignedClaims(token).getPayload();
    }

    public long expirationSeconds() { return expirationSeconds; }
}
