package com.sccothe.fridgeclear.ai.api;

import com.sccothe.fridgeclear.ai.service.SystemAiConfigService;
import com.sccothe.fridgeclear.common.api.ApiResponse;
import com.sccothe.fridgeclear.common.api.RequestIdFilter;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/ai/config")
@Tag(name = "全局 AI 配置", description = "平台级 AI Provider 配置与连接测试（仅 ADMIN）")
@SecurityRequirement(name = "Authorization")
public class SystemAiConfigController {
    private final SystemAiConfigService service;

    public SystemAiConfigController(SystemAiConfigService service) { this.service = service; }

    @GetMapping
    @Operation(summary = "获取全局 AI 配置")
    public ApiResponse<?> get(HttpServletRequest request) { return success(service.get(), request); }

    @PutMapping
    @Operation(summary = "保存全局 AI 配置")
    public ApiResponse<?> update(@Valid @RequestBody SystemAiConfigDtos.UpdateRequest body, HttpServletRequest request) {
        return success(service.update(body), request);
    }

    @PostMapping("/models")
    @Operation(summary = "获取全局 AI 配置模型列表")
    public ApiResponse<?> models(@Valid @RequestBody SystemAiConfigDtos.ModelsRequest body, HttpServletRequest request) {
        return success(service.models(body), request);
    }

    @PostMapping("/test")
    @Operation(summary = "测试全局 AI 配置连接")
    public ApiResponse<?> test(HttpServletRequest request) { return success(service.test(), request); }

    private <T> ApiResponse<T> success(T data, HttpServletRequest request) {
        Object value = request.getAttribute(RequestIdFilter.REQUEST_ID_ATTRIBUTE);
        String requestId = value == null ? "req_" + UUID.randomUUID() : value.toString();
        return ApiResponse.success(data, requestId);
    }
}
