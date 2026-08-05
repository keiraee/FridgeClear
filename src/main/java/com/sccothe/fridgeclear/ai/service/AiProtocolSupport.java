package com.sccothe.fridgeclear.ai.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sccothe.fridgeclear.ai.api.AiProviderDtos;
import com.sccothe.fridgeclear.ai.domain.AiProtocol;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

/**
 * 与协议相关的无状态工具：模型发现、Base URL 归一化、异常脱敏。
 * 用户自配 Provider 与全局 AI 配置共用，后续可在此扩展为协议适配器。
 */
@Service
public class AiProtocolSupport {
    private final ObjectMapper objectMapper;

    public AiProtocolSupport(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public List<AiProviderDtos.ModelItem> fetchModels(AiProtocol protocol, String baseUrl, String apiKey) {
        RestClient client = RestClient.builder().baseUrl(baseUrl).build();
        RestClient.RequestHeadersSpec<?> request;
        if (protocol == AiProtocol.ANTHROPIC_MESSAGES) {
            request = client.get().uri("/v1/models")
                    .header("x-api-key", apiKey)
                    .header("anthropic-version", "2023-06-01");
        } else if (protocol == AiProtocol.GEMINI_NATIVE) {
            request = client.get().uri(uriBuilder -> uriBuilder.path("/v1beta/models").queryParam("key", apiKey).build());
        } else {
            request = client.get().uri("/models").header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey);
        }
        String body = request.retrieve().body(String.class);
        return parseModels(body);
    }

    public List<AiProviderDtos.ModelItem> parseModels(String body) {
        try {
            JsonNode root = objectMapper.readTree(body);
            JsonNode data = root.has("data") ? root.get("data") : root.path("models");
            List<AiProviderDtos.ModelItem> result = new ArrayList<>();
            if (data.isArray()) {
                for (JsonNode item : data) {
                    String id = item.has("id") ? item.get("id").asText() : item.path("name").asText();
                    if (!id.isBlank()) result.add(new AiProviderDtos.ModelItem(id, id));
                }
            }
            return result;
        } catch (Exception exception) {
            throw new IllegalStateException("模型列表响应不是有效 JSON", exception);
        }
    }

    public String normalizeBaseUrl(String baseUrl) {
        String normalized = baseUrl.trim();
        while (normalized.endsWith("/")) normalized = normalized.substring(0, normalized.length() - 1);
        try { URI.create(normalized); } catch (Exception exception) { throw new IllegalArgumentException("Base URL 格式不正确"); }
        return normalized;
    }

    public String safeMessage(Exception exception) {
        String message = exception.getMessage();
        return message == null || message.isBlank() ? "连接失败" : message.replaceAll("(?i)(api[_-]?key|authorization|token)[^,; ]*", "$1=***");
    }
}
