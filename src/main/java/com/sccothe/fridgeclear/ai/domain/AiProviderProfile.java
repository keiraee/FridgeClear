package com.sccothe.fridgeclear.ai.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "ai_provider_profile", uniqueConstraints = @UniqueConstraint(
        name = "uk_ai_provider_user_name", columnNames = {"user_id", "name"}))
public class AiProviderProfile {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(nullable = false) private Long userId;
    @Column(nullable = false, length = 128) private String name;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 32) private AiProtocol protocol;
    @Column(nullable = false, length = 512) private String baseUrl;
    @Column(name = "api_key_ciphertext", nullable = false, columnDefinition = "text") private String apiKeyCiphertext;
    @Column(nullable = false, length = 128) private String modelName = "";
    @Column(nullable = false) private boolean enabled;
    @Column(nullable = false) private boolean active;
    @Column(nullable = false) private LocalDateTime createdAt;
    @Column(nullable = false) private LocalDateTime updatedAt;

    @PrePersist void onCreate() { var now = LocalDateTime.now(); createdAt = now; updatedAt = now; }
    @PreUpdate void onUpdate() { updatedAt = LocalDateTime.now(); }
    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long value) { userId = value; }
    public String getName() { return name; }
    public void setName(String value) { name = value; }
    public AiProtocol getProtocol() { return protocol; }
    public void setProtocol(AiProtocol value) { protocol = value; }
    public String getBaseUrl() { return baseUrl; }
    public void setBaseUrl(String value) { baseUrl = value; }
    public String getApiKeyCiphertext() { return apiKeyCiphertext; }
    public void setApiKeyCiphertext(String value) { apiKeyCiphertext = value; }
    public String getModelName() { return modelName; }
    public void setModelName(String value) { modelName = value; }
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean value) { enabled = value; }
    public boolean isActive() { return active; }
    public void setActive(boolean value) { active = value; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
