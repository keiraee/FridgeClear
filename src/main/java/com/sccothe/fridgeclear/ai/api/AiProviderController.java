package com.sccothe.fridgeclear.ai.api;

import com.sccothe.fridgeclear.common.api.ApiResponse;
import com.sccothe.fridgeclear.common.api.RequestIdFilter;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/ai/providers")
@Tag(name = "AI 模型配置", description = "多 AI Provider 配置、切换和连接测试")
@SecurityRequirement(name = "Authorization")
public class AiProviderController {
    private final com.sccothe.fridgeclear.ai.service.AiProviderService service;

    public AiProviderController(com.sccothe.fridgeclear.ai.service.AiProviderService service) { this.service = service; }

    @GetMapping
    @Operation(summary = "获取 AI Provider 列表")
    public ApiResponse<?> list(HttpServletRequest request) { return success(service.list(), request); }

    @PostMapping
    @Operation(summary = "新增 AI Provider")
    public ApiResponse<?> create(@Valid @RequestBody AiProviderDtos.CreateRequest body, HttpServletRequest request) { return success(service.create(body), request); }

    @PutMapping("/{id}")
    @Operation(summary = "修改 AI Provider")
    public ApiResponse<?> update(@PathVariable Long id, @Valid @RequestBody AiProviderDtos.UpdateRequest body, HttpServletRequest request) { return success(service.update(id, body), request); }

    @PutMapping("/{id}/activate")
    @Operation(summary = "激活 AI Provider")
    public ApiResponse<?> activate(@PathVariable Long id, HttpServletRequest request) { return success(service.activate(id), request); }

    @GetMapping("/{id}/models")
    @Operation(summary = "获取 Provider 模型列表")
    public ApiResponse<?> models(@PathVariable Long id, HttpServletRequest request) { return success(service.models(id), request); }

    @PostMapping("/{id}/test")
    @Operation(summary = "测试 Provider 连接")
    public ApiResponse<?> test(@PathVariable Long id, HttpServletRequest request) { return success(service.test(id), request); }

    private <T> ApiResponse<T> success(T data, HttpServletRequest request) {
        Object value = request.getAttribute(RequestIdFilter.REQUEST_ID_ATTRIBUTE);
        String requestId = value == null ? "req_" + UUID.randomUUID() : value.toString();
        return ApiResponse.success(data, requestId);
    }
}
