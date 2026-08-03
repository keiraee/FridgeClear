package com.sccothe.fridgeclear.auth.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "user_account")
public class UserAccount {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(nullable = false, length = 190, unique = true) private String email;
    @Column(nullable = false, length = 100) private String passwordHash;
    @Column(nullable = false, length = 64) private String nickname;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 32) private UserRole role;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 32) private UserStatus status;
    @Column(nullable = false) private LocalDateTime createdAt;
    @Column(nullable = false) private LocalDateTime updatedAt;

    @PrePersist void onCreate() { var now = LocalDateTime.now(); createdAt = now; updatedAt = now; }
    @PreUpdate void onUpdate() { updatedAt = LocalDateTime.now(); }
    public Long getId() { return id; }
    public String getEmail() { return email; }
    public void setEmail(String value) { email = value; }
    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String value) { passwordHash = value; }
    public String getNickname() { return nickname; }
    public void setNickname(String value) { nickname = value; }
    public UserRole getRole() { return role; }
    public void setRole(UserRole value) { role = value; }
    public UserStatus getStatus() { return status; }
    public void setStatus(UserStatus value) { status = value; }

    public enum UserRole { USER, ADMIN }
    public enum UserStatus { ACTIVE, DISABLED }
}
