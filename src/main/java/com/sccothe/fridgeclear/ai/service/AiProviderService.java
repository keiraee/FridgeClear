package com.sccothe.fridgeclear.ai.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sccothe.fridgeclear.ai.api.AiProviderDtos;
import com.sccothe.fridgeclear.ai.domain.AiProviderProfile;
import com.sccothe.fridgeclear.ai.domain.AiProtocol;
import com.sccothe.fridgeclear.ai.repository.AiProviderProfileRepository;
import com.sccothe.fridgeclear.common.api.ResourceNotFoundException;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

@Service
@Transactional
public class AiProviderService {
    private static final long DEMO_USER_ID = 1L;
    private final AiProviderProfileRepository repository;
    private final AesGcmCrypto crypto;
    private final ObjectMapper objectMapper;

    public AiProviderService(AiProviderProfileRepository repository, AesGcmCrypto crypto, ObjectMapper objectMapper) {
        this.repository = repository;
        this.crypto = crypto;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public List<AiProviderDtos.Response> list() {
        return repository.findByUserIdOrderByActiveDescNameAsc(DEMO_USER_ID).stream().map(this::toResponse).toList();
    }

    public AiProviderDtos.Response create(AiProviderDtos.CreateRequest request) {
        AiProviderProfile profile = new AiProviderProfile();
        profile.setUserId(DEMO_USER_ID);
        profile.setActive(repository.findByUserIdAndActiveTrue(DEMO_USER_ID).isEmpty());
        profile.setEnabled(true);
        apply(profile, request.name(), request.protocol(), request.baseUrl(), request.modelName());
        profile.setApiKeyCiphertext(crypto.encrypt(request.apiKey()));
        AiProviderProfile saved = repository.save(profile);
        discoverModelIfMissing(saved);
        return toResponse(saved);
    }

    public AiProviderDtos.Response update(Long id, AiProviderDtos.UpdateRequest request) {
        AiProviderProfile profile = findOwned(id);
        apply(profile, request.name(), request.protocol(), request.baseUrl(), request.modelName());
        profile.setEnabled(request.enabled());
        if (request.apiKey() != null && !request.apiKey().isBlank()) profile.setApiKeyCiphertext(crypto.encrypt(request.apiKey()));
        discoverModelIfMissing(profile);
        return toResponse(profile);
    }

    public AiProviderDtos.Response activate(Long id) {
        AiProviderProfile target = findOwned(id);
        repository.findByUserIdOrderByActiveDescNameAsc(DEMO_USER_ID).forEach(item -> item.setActive(false));
        target.setActive(true);
        target.setEnabled(true);
        return toResponse(target);
    }

    @Transactional(readOnly = true)
    public AiProviderDtos.ModelListResponse models(Long id) {
        return new AiProviderDtos.ModelListResponse(fetchModels(findOwned(id)));
    }

    @Transactional(readOnly = true)
    public AiProviderDtos.ConnectionTestResponse test(Long id) {
        try {
            List<AiProviderDtos.ModelItem> models = fetchModels(findOwned(id));
            return new AiProviderDtos.ConnectionTestResponse(true, "连接成功", models.size());
        } catch (Exception exception) {
            return new AiProviderDtos.ConnectionTestResponse(false, safeMessage(exception), 0);
        }
    }

    private List<AiProviderDtos.ModelItem> fetchModels(AiProviderProfile profile) {
        String apiKey = crypto.decrypt(profile.getApiKeyCiphertext());
        RestClient client = RestClient.builder().baseUrl(profile.getBaseUrl()).build();
        RestClient.RequestHeadersSpec<?> request;
        if (profile.getProtocol() == AiProtocol.ANTHROPIC_MESSAGES) {
            request = client.get().uri("/v1/models")
                    .header("x-api-key", apiKey)
                    .header("anthropic-version", "2023-06-01");
        } else if (profile.getProtocol() == AiProtocol.GEMINI_NATIVE) {
            request = client.get().uri(uriBuilder -> uriBuilder.path("/v1beta/models").queryParam("key", apiKey).build());
        } else {
            request = client.get().uri("/models").header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey);
        }
        String body = request.retrieve().body(String.class);
        return parseModels(body);
    }

    private List<AiProviderDtos.ModelItem> parseModels(String body) {
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

    private void apply(AiProviderProfile profile, String name, AiProtocol protocol, String baseUrl, String modelName) {
        profile.setName(name.trim());
        profile.setProtocol(protocol);
        profile.setBaseUrl(normalizeBaseUrl(baseUrl));
        if (modelName != null && !modelName.isBlank()) profile.setModelName(modelName.trim());
    }

    private void discoverModelIfMissing(AiProviderProfile profile) {
        if (profile.getModelName() != null && !profile.getModelName().isBlank()) return;
        try {
            List<AiProviderDtos.ModelItem> models = fetchModels(profile);
            if (!models.isEmpty()) profile.setModelName(models.get(0).id());
        } catch (Exception ignored) {
            // Provider may not expose a model-list endpoint; keep the profile usable for later manual selection.
        }
    }

    private String normalizeBaseUrl(String baseUrl) {
        String normalized = baseUrl.trim();
        while (normalized.endsWith("/")) normalized = normalized.substring(0, normalized.length() - 1);
        try { URI.create(normalized); } catch (Exception exception) { throw new IllegalArgumentException("Base URL 格式不正确"); }
        return normalized;
    }

    private AiProviderProfile findOwned(Long id) {
        return repository.findByIdAndUserId(id, DEMO_USER_ID)
                .orElseThrow(() -> new ResourceNotFoundException("AI Provider 不存在: " + id));
    }

    private AiProviderDtos.Response toResponse(AiProviderProfile profile) {
        String masked = "已配置";
        return new AiProviderDtos.Response(profile.getId(), profile.getName(), profile.getProtocol(), profile.getBaseUrl(),
                profile.getModelName(), profile.isEnabled(), profile.isActive(), true, masked);
    }

    private String safeMessage(Exception exception) {
        String message = exception.getMessage();
        return message == null || message.isBlank() ? "连接失败" : message.replaceAll("(?i)(api[_-]?key|authorization|token)[^,; ]*", "$1=***");
    }
}
