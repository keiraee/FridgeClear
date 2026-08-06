package com.sccothe.fridgeclear.ai.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sccothe.fridgeclear.ai.domain.AiProtocol;
import com.sccothe.fridgeclear.ai.domain.AiProviderProfile;
import com.sccothe.fridgeclear.ai.domain.SystemAiConfig;
import com.sccothe.fridgeclear.ai.repository.AiProviderProfileRepository;
import com.sccothe.fridgeclear.ai.repository.SystemAiConfigRepository;
import com.sccothe.fridgeclear.auth.service.CurrentUser;
import com.sccothe.fridgeclear.common.api.AiServiceUnavailableException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class AiChatGateway {
    private final AiProviderProfileRepository providerRepository;
    private final SystemAiConfigRepository systemConfigRepository;
    private final AesGcmCrypto crypto;
    private final ObjectMapper objectMapper;
    private final boolean byokEnabled;

    public AiChatGateway(AiProviderProfileRepository providerRepository,
                         SystemAiConfigRepository systemConfigRepository,
                         AesGcmCrypto crypto, ObjectMapper objectMapper,
                         @Value("${fridgeclear.ai.byok-enabled:false}") boolean byokEnabled) {
        this.providerRepository = providerRepository;
        this.systemConfigRepository = systemConfigRepository;
        this.crypto = crypto;
        this.objectMapper = objectMapper;
        this.byokEnabled = byokEnabled;
    }

    public ChatResult complete(String systemPrompt, String userPrompt) {
        ResolvedConfig config = resolveConfig();
        return completeWith(config.protocol(), config.baseUrl(), config.modelName(), config.apiKey(),
                systemPrompt, userPrompt);
    }

    /** 提交备餐任务前校验，避免创建注定失败的后台任务。 */
    public void assertAvailable() {
        resolveConfig();
    }

    private ResolvedConfig resolveConfig() {
        SystemAiConfig systemConfig = systemConfigRepository.findSingleton().orElse(null);
        if (systemConfig != null && systemConfig.isEnabled()) {
            return resolvedFromSystem(systemConfig);
        }
        if (!byokEnabled) {
            throw new AiServiceUnavailableException("AI 服务暂不可用，请稍后再试");
        }
        AiProviderProfile provider = providerRepository.findByUserIdAndActiveTrue(CurrentUser.id())
                .filter(AiProviderProfile::isEnabled)
                .orElseThrow(() -> new AiServiceUnavailableException("AI 服务暂不可用，请稍后再试"));
        return new ResolvedConfig(provider.getProtocol(), provider.getBaseUrl(), provider.getModelName(),
                crypto.decrypt(provider.getApiKeyCiphertext()));
    }

    private ResolvedConfig resolvedFromSystem(SystemAiConfig systemConfig) {
        if (systemConfig.getProtocol() != AiProtocol.OPENAI_CHAT) {
            throw new AiServiceUnavailableException("当前备餐计划仅支持 OpenAI 兼容接口，请在管理端选择 OPENAI_CHAT 协议");
        }
        if (systemConfig.getModelName() == null || systemConfig.getModelName().isBlank()) {
            throw new AiServiceUnavailableException("AI 服务暂不可用，请先在管理端配置模型");
        }
        if (systemConfig.getApiKeyCiphertext() == null || systemConfig.getApiKeyCiphertext().isBlank()) {
            throw new AiServiceUnavailableException("AI 服务暂不可用，请先在管理端配置 API Key");
        }
        return new ResolvedConfig(systemConfig.getProtocol(), systemConfig.getBaseUrl(), systemConfig.getModelName(),
                crypto.decrypt(systemConfig.getApiKeyCiphertext()));
    }

    private record ResolvedConfig(AiProtocol protocol, String baseUrl, String modelName, String apiKey) {}

    private ChatResult completeWith(AiProtocol protocol, String baseUrl, String modelName, String apiKey,
                                    String systemPrompt, String userPrompt) {
        if (protocol != AiProtocol.OPENAI_CHAT) {
            throw new AiServiceUnavailableException("当前备餐计划仅支持 OpenAI 兼容接口，请在管理端选择 OPENAI_CHAT 协议");
        }
        if (modelName == null || modelName.isBlank()) {
            throw new AiServiceUnavailableException("AI 服务暂不可用，请稍后再试");
        }

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", modelName);
        body.put("temperature", 0.2);
        body.put("messages", new Object[]{
                Map.of("role", "system", "content", systemPrompt),
                Map.of("role", "user", "content", userPrompt)
        });
        String response = RestClient.builder().baseUrl(baseUrl).build()
                .post().uri("/chat/completions")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .body(body)
                .retrieve().body(String.class);
        try {
            JsonNode content = objectMapper.readTree(response).path("choices").path(0).path("message").path("content");
            if (!content.isTextual() || content.asText().isBlank()) throw new IllegalStateException("AI 没有返回有效内容");
            return new ChatResult(modelName, content.asText());
        } catch (Exception exception) {
            throw new IllegalStateException("AI 响应格式不正确", exception);
        }
    }

    public record ChatResult(String modelName, String content) {}
}
