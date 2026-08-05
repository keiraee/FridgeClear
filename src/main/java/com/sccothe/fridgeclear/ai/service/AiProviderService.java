package com.sccothe.fridgeclear.ai.service;

import com.sccothe.fridgeclear.ai.api.AiProviderDtos;
import com.sccothe.fridgeclear.ai.domain.AiProviderProfile;
import com.sccothe.fridgeclear.ai.domain.AiProtocol;
import com.sccothe.fridgeclear.ai.repository.AiProviderProfileRepository;
import com.sccothe.fridgeclear.auth.service.CurrentUser;
import com.sccothe.fridgeclear.common.api.FeatureDisabledException;
import com.sccothe.fridgeclear.common.api.ResourceNotFoundException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class AiProviderService {
    private final AiProviderProfileRepository repository;
    private final AesGcmCrypto crypto;
    private final AiProtocolSupport protocolSupport;
    private final boolean byokEnabled;

    public AiProviderService(AiProviderProfileRepository repository, AesGcmCrypto crypto, AiProtocolSupport protocolSupport,
                             @Value("${fridgeclear.ai.byok-enabled:false}") boolean byokEnabled) {
        this.repository = repository;
        this.crypto = crypto;
        this.protocolSupport = protocolSupport;
        this.byokEnabled = byokEnabled;
    }

    private void ensureByokEnabled() {
        if (!byokEnabled) throw new FeatureDisabledException("用户自配 AI Provider 功能未开放");
    }

    @Transactional(readOnly = true)
    public List<AiProviderDtos.Response> list() {
        ensureByokEnabled();
        return repository.findByUserIdOrderByActiveDescNameAsc(CurrentUser.id()).stream().map(this::toResponse).toList();
    }

    public AiProviderDtos.Response create(AiProviderDtos.CreateRequest request) {
        ensureByokEnabled();
        AiProviderProfile profile = new AiProviderProfile();
        Long userId = CurrentUser.id();
        profile.setUserId(userId);
        profile.setActive(repository.findByUserIdAndActiveTrue(userId).isEmpty());
        profile.setEnabled(true);
        apply(profile, request.name(), request.protocol(), request.baseUrl(), request.modelName());
        profile.setApiKeyCiphertext(crypto.encrypt(request.apiKey()));
        AiProviderProfile saved = repository.save(profile);
        discoverModelIfMissing(saved);
        return toResponse(saved);
    }

    public AiProviderDtos.Response update(Long id, AiProviderDtos.UpdateRequest request) {
        ensureByokEnabled();
        AiProviderProfile profile = findOwned(id);
        apply(profile, request.name(), request.protocol(), request.baseUrl(), request.modelName());
        profile.setEnabled(request.enabled());
        if (request.apiKey() != null && !request.apiKey().isBlank()) profile.setApiKeyCiphertext(crypto.encrypt(request.apiKey()));
        discoverModelIfMissing(profile);
        return toResponse(profile);
    }

    public AiProviderDtos.Response activate(Long id) {
        ensureByokEnabled();
        AiProviderProfile target = findOwned(id);
        repository.findByUserIdOrderByActiveDescNameAsc(CurrentUser.id()).forEach(item -> item.setActive(false));
        target.setActive(true);
        target.setEnabled(true);
        return toResponse(target);
    }

    @Transactional(readOnly = true)
    public AiProviderDtos.ModelListResponse models(Long id) {
        ensureByokEnabled();
        return new AiProviderDtos.ModelListResponse(fetchModels(findOwned(id)));
    }

    @Transactional(readOnly = true)
    public AiProviderDtos.ConnectionTestResponse test(Long id) {
        ensureByokEnabled();
        try {
            List<AiProviderDtos.ModelItem> models = fetchModels(findOwned(id));
            return new AiProviderDtos.ConnectionTestResponse(true, "连接成功", models.size());
        } catch (Exception exception) {
            return new AiProviderDtos.ConnectionTestResponse(false, protocolSupport.safeMessage(exception), 0);
        }
    }

    private List<AiProviderDtos.ModelItem> fetchModels(AiProviderProfile profile) {
        return protocolSupport.fetchModels(profile.getProtocol(), profile.getBaseUrl(), crypto.decrypt(profile.getApiKeyCiphertext()));
    }

    private void apply(AiProviderProfile profile, String name, AiProtocol protocol, String baseUrl, String modelName) {
        profile.setName(name.trim());
        profile.setProtocol(protocol);
        profile.setBaseUrl(protocolSupport.normalizeBaseUrl(baseUrl));
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

    private AiProviderProfile findOwned(Long id) {
        return repository.findByIdAndUserId(id, CurrentUser.id())
                .orElseThrow(() -> new ResourceNotFoundException("AI Provider 不存在: " + id));
    }

    private AiProviderDtos.Response toResponse(AiProviderProfile profile) {
        String masked = "已配置";
        return new AiProviderDtos.Response(profile.getId(), profile.getName(), profile.getProtocol(), profile.getBaseUrl(),
                profile.getModelName(), profile.isEnabled(), profile.isActive(), true, masked);
    }
}
