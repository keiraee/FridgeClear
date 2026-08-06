package com.sccothe.fridgeclear.telemetry.api;

import com.sccothe.fridgeclear.common.api.ApiResponse;
import com.sccothe.fridgeclear.common.api.RequestIdFilter;
import com.sccothe.fridgeclear.common.util.ClientNetworkUtils;
import com.sccothe.fridgeclear.telemetry.service.AccessLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/telemetry")
@Tag(name = "访问遥测", description = "记录客户端访问环境与定位信息")
public class AccessLogController {
    private final AccessLogService service;

    public AccessLogController(AccessLogService service) {
        this.service = service;
    }

    @PostMapping("/access")
    @Operation(summary = "上报访问遥测（静默，无需登录）")
    public ApiResponse<Void> record(
            @Valid @RequestBody AccessLogDtos.RecordRequest request,
            HttpServletRequest httpRequest
    ) {
        service.record(request, httpRequest, ClientNetworkUtils.resolveClientIp(httpRequest));
        return ApiResponse.success(null, requestId(httpRequest));
    }

    private String requestId(HttpServletRequest request) {
        Object value = request.getAttribute(RequestIdFilter.REQUEST_ID_ATTRIBUTE);
        return value == null ? "req_" + UUID.randomUUID() : value.toString();
    }
}
