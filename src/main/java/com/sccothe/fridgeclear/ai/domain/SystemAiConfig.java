package com.sccothe.fridgeclear.ai.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * 平台级全局 AI 配置（单行语义，Service 层固定使用 id=1）。
 * 普通用户生成备餐计划时优先使用该配置，未配置/未启用时回退到用户自配 Provider。
 */
@Entity
@Table(name = "system_ai_config")
public class SystemAiConfig {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "provider_name", nullable = false, length = 128) private String providerName;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 32) private AiProtocol protocol;
    @Column(name = "base_url", nullable = false, length = 512) private String baseUrl;
    @Column(name = "api_key_ciphertext", nullable = false, columnDefinition = "text") private String apiKeyCiphertext;
    @Column(name = "model_name", nullable = false, length = 128) private String modelName = "";
    @Column(nullable = false) private boolean enabled;
    @Column(nullable = false) private LocalDateTime createdAt;
    @Column(nullable = false) private LocalDateTime updatedAt;

    @PrePersist void onCreate() { var now = LocalDateTime.now(); createdAt = now; updatedAt = now; }
    @PreUpdate void onUpdate() { updatedAt = LocalDateTime.now(); }
    public Long getId() { return id; }
    public String getProviderName() { return providerName; }
    public void setProviderName(String value) { providerName = value; }
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
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
