package com.sccothe.fridgeclear.ai.api;

import com.sccothe.fridgeclear.ai.domain.AiProtocol;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public final class SystemAiConfigDtos {
    private SystemAiConfigDtos() {
    }

    @Schema(name = "SystemAiConfigResponse", description = "全局 AI 配置响应（API Key 仅返回是否已配置）")
    public record Response(String providerName, AiProtocol protocol, String baseUrl,
                           String modelName, boolean enabled, boolean apiKeyConfigured) {
    }

    @Schema(name = "SystemAiConfigUpdateRequest", description = "保存全局 AI 配置请求")
    public record UpdateRequest(
            @NotBlank @Size(max = 128) String providerName,
            @NotNull AiProtocol protocol,
            @NotBlank @Size(max = 512) String baseUrl,
            String apiKey,
            @Size(max = 128) String modelName,
            boolean enabled
    ) {
    }

    public record TestResponse(boolean success, String message, int modelCount) {
    }

    @Schema(name = "SystemAiConfigModelsRequest", description = "获取全局 AI 配置模型列表请求")
    public record ModelsRequest(
            @NotNull AiProtocol protocol,
            @NotBlank @Size(max = 512) String baseUrl,
            String apiKey
    ) {
    }
}
