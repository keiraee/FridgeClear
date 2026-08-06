package com.sccothe.fridgeclear.telemetry.api;

import com.sccothe.fridgeclear.common.api.ApiResponse;
import com.sccothe.fridgeclear.common.api.RequestIdFilter;
import com.sccothe.fridgeclear.telemetry.service.AccessLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/access-logs")
@Tag(name = "访问日志管理", description = "查看访问遥测记录（仅 ADMIN）")
@SecurityRequirement(name = "Authorization")
public class AdminAccessLogController {
    private final AccessLogService service;

    public AdminAccessLogController(AccessLogService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "分页查询访问日志")
    public ApiResponse<AccessLogDtos.ListResponse> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            HttpServletRequest request
    ) {
        return ApiResponse.success(service.list(page, size), requestId(request));
    }

    @GetMapping("/stats")
    @Operation(summary = "访问日志统计概览")
    public ApiResponse<AccessLogDtos.StatsResponse> stats(HttpServletRequest request) {
        return ApiResponse.success(service.stats(), requestId(request));
    }

    private String requestId(HttpServletRequest request) {
        Object value = request.getAttribute(RequestIdFilter.REQUEST_ID_ATTRIBUTE);
        return value == null ? "req_" + UUID.randomUUID() : value.toString();
    }
}
