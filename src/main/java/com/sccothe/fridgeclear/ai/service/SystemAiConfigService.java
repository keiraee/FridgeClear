package com.sccothe.fridgeclear.ai.service;

import com.sccothe.fridgeclear.ai.api.AiProviderDtos;
import com.sccothe.fridgeclear.ai.api.SystemAiConfigDtos;
import com.sccothe.fridgeclear.ai.domain.AiProtocol;
import com.sccothe.fridgeclear.ai.domain.SystemAiConfig;
import com.sccothe.fridgeclear.ai.repository.SystemAiConfigRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class SystemAiConfigService {
    private final SystemAiConfigRepository repository;
    private final AesGcmCrypto crypto;
    private final AiProtocolSupport protocolSupport;

    public SystemAiConfigService(SystemAiConfigRepository repository, AesGcmCrypto crypto, AiProtocolSupport protocolSupport) {
        this.repository = repository;
        this.crypto = crypto;
        this.protocolSupport = protocolSupport;
    }

    @Transactional(readOnly = true)
    public SystemAiConfigDtos.Response get() {
        return repository.findSingleton()
                .map(this::toResponse)
                .orElseGet(() -> new SystemAiConfigDtos.Response("", AiProtocol.OPENAI_CHAT, "", "", false, false));
    }

    public SystemAiConfigDtos.Response update(SystemAiConfigDtos.UpdateRequest request) {
        if (request.protocol() != AiProtocol.OPENAI_CHAT) {
            throw new IllegalArgumentException("备餐计划当前仅支持 OPENAI_CHAT 协议，请选择 OpenAI 兼容接口");
        }
        SystemAiConfig config = repository.findSingleton().orElseGet(SystemAiConfig::new);
        config.setProviderName(request.providerName().trim());
        config.setProtocol(request.protocol());
        config.setBaseUrl(protocolSupport.normalizeBaseUrl(request.baseUrl()));
        config.setModelName(request.modelName() == null ? "" : request.modelName().trim());
        config.setEnabled(request.enabled());
        if (request.apiKey() != null && !request.apiKey().isBlank()) {
            config.setApiKeyCiphertext(crypto.encrypt(request.apiKey()));
        } else if (config.getApiKeyCiphertext() == null || config.getApiKeyCiphertext().isBlank()) {
            throw new IllegalArgumentException("请填写 API Key");
        }
        if (config.getModelName().isBlank()) discoverModelIfMissing(config);
        SystemAiConfig saved = repository.save(config);
        repository.deleteAllExcept(saved.getId());
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<AiProviderDtos.ModelItem> models(SystemAiConfigDtos.ModelsRequest request) {
        String apiKey = request.apiKey();
        if (apiKey == null || apiKey.isBlank()) {
            SystemAiConfig saved = repository.findSingleton()
                    .orElseThrow(() -> new IllegalArgumentException("请填写 API Key 以获取模型列表"));
            apiKey = crypto.decrypt(saved.getApiKeyCiphertext());
        }
        return protocolSupport.fetchModels(request.protocol(), protocolSupport.normalizeBaseUrl(request.baseUrl()), apiKey);
    }

    @Transactional(readOnly = true)
    public SystemAiConfigDtos.TestResponse test() {
        SystemAiConfig config = repository.findSingleton()
                .orElseThrow(() -> new IllegalArgumentException("全局 AI 配置尚未保存，请先保存后再测试"));
        try {
            List<AiProviderDtos.ModelItem> models = protocolSupport.fetchModels(
                    config.getProtocol(), config.getBaseUrl(), crypto.decrypt(config.getApiKeyCiphertext()));
            return new SystemAiConfigDtos.TestResponse(true, "连接成功", models.size());
        } catch (Exception exception) {
            return new SystemAiConfigDtos.TestResponse(false, protocolSupport.safeMessage(exception), 0);
        }
    }

    private void discoverModelIfMissing(SystemAiConfig config) {
        try {
            List<AiProviderDtos.ModelItem> models = protocolSupport.fetchModels(
                    config.getProtocol(), config.getBaseUrl(), crypto.decrypt(config.getApiKeyCiphertext()));
            if (!models.isEmpty()) config.setModelName(models.get(0).id());
        } catch (Exception ignored) {
            // Provider may not expose a model-list endpoint; keep the model blank for later manual selection.
        }
    }

    private SystemAiConfigDtos.Response toResponse(SystemAiConfig config) {
        return new SystemAiConfigDtos.Response(config.getProviderName(), config.getProtocol(), config.getBaseUrl(),
                config.getModelName(), config.isEnabled(),
                config.getApiKeyCiphertext() != null && !config.getApiKeyCiphertext().isBlank());
    }
}
