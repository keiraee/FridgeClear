package com.sccothe.fridgeclear.ai.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sccothe.fridgeclear.ai.domain.AiProtocol;
import com.sccothe.fridgeclear.ai.domain.AiProviderProfile;
import com.sccothe.fridgeclear.ai.repository.AiProviderProfileRepository;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class AiChatGateway {
    private static final long DEMO_USER_ID = 1L;
    private final AiProviderProfileRepository providerRepository;
    private final AesGcmCrypto crypto;
    private final ObjectMapper objectMapper;

    public AiChatGateway(AiProviderProfileRepository providerRepository, AesGcmCrypto crypto, ObjectMapper objectMapper) {
        this.providerRepository = providerRepository;
        this.crypto = crypto;
        this.objectMapper = objectMapper;
    }

    public ChatResult complete(String systemPrompt, String userPrompt) {
        AiProviderProfile provider = providerRepository.findByUserIdAndActiveTrue(DEMO_USER_ID)
                .filter(AiProviderProfile::isEnabled)
                .orElseThrow(() -> new IllegalStateException("请先配置并激活 AI Provider"));
        if (provider.getProtocol() != AiProtocol.OPENAI_CHAT) {
            throw new IllegalStateException("当前备餐计划第一版仅支持 OPENAI_CHAT 协议");
        }
        if (provider.getModelName() == null || provider.getModelName().isBlank()) {
            throw new IllegalStateException("当前 Provider 尚未选择模型");
        }

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", provider.getModelName());
        body.put("temperature", 0.2);
        body.put("messages", new Object[]{
                Map.of("role", "system", "content", systemPrompt),
                Map.of("role", "user", "content", userPrompt)
        });
        String response = RestClient.builder().baseUrl(provider.getBaseUrl()).build()
                .post().uri("/chat/completions")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + crypto.decrypt(provider.getApiKeyCiphertext()))
                .body(body)
                .retrieve().body(String.class);
        try {
            JsonNode content = objectMapper.readTree(response).path("choices").path(0).path("message").path("content");
            if (!content.isTextual() || content.asText().isBlank()) throw new IllegalStateException("AI 没有返回有效内容");
            return new ChatResult(provider.getModelName(), content.asText());
        } catch (Exception exception) {
            throw new IllegalStateException("AI 响应格式不正确", exception);
        }
    }

    public record ChatResult(String modelName, String content) {}
}
