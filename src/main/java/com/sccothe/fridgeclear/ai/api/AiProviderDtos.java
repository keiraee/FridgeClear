package com.sccothe.fridgeclear.ai.api;

import com.sccothe.fridgeclear.ai.domain.AiProtocol;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public final class AiProviderDtos {
    private AiProviderDtos() {
    }

    @Schema(name = "AiProviderCreateRequest", description = "新增 AI Provider 请求")
    public record CreateRequest(
            @NotBlank @Size(max = 128) String name,
            @NotNull AiProtocol protocol,
            @NotBlank @Size(max = 512) String baseUrl,
            @NotBlank String apiKey,
            @Size(max = 128) String modelName
    ) {
    }

    @Schema(name = "AiProviderUpdateRequest", description = "修改 AI Provider 请求")
    public record UpdateRequest(
            @NotBlank @Size(max = 128) String name,
            @NotNull AiProtocol protocol,
            @NotBlank @Size(max = 512) String baseUrl,
            String apiKey,
            @Size(max = 128) String modelName,
            boolean enabled
    ) {
    }

    @Schema(name = "AiProviderResponse", description = "AI Provider 配置响应")
    public record Response(Long id, String name, AiProtocol protocol, String baseUrl,
                           String modelName, boolean enabled, boolean active,
                           boolean apiKeyConfigured, String apiKeyPreview) {
    }

    public record ModelItem(String id, String name) {
    }

    public record ModelListResponse(List<ModelItem> models) {
    }

    public record ConnectionTestResponse(boolean success, String message, int modelCount) {
    }
}
