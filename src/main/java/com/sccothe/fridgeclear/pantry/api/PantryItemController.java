package com.sccothe.fridgeclear.pantry.api;

import com.sccothe.fridgeclear.common.api.ApiResponse;
import com.sccothe.fridgeclear.common.api.RequestIdFilter;
import com.sccothe.fridgeclear.pantry.domain.PantryItemStatus;
import com.sccothe.fridgeclear.pantry.service.PantryItemService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/pantry-items")
@Tag(name = "Pantry", description = "用户食材库存")
public class PantryItemController {
    private final PantryItemService service;

    public PantryItemController(PantryItemService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "获取食材库存")
    public ApiResponse<?> list(
            @RequestParam(required = false) PantryItemStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            HttpServletRequest request
    ) {
        int safeSize = Math.min(Math.max(size, 1), 100);
        var result = service.list(status, PageRequest.of(Math.max(page, 0), safeSize, Sort.by("expireDate").ascending()));
        return ApiResponse.success(result, requestId(request));
    }

    @PostMapping
    @Operation(summary = "新增库存食材")
    public ApiResponse<PantryItemDtos.Response> create(
            @Valid @RequestBody PantryItemDtos.CreateRequest body,
            HttpServletRequest request
    ) {
        return ApiResponse.success(service.create(body), requestId(request));
    }

    @PutMapping("/{id}")
    @Operation(summary = "修改库存食材")
    public ApiResponse<PantryItemDtos.Response> update(
            @PathVariable Long id,
            @Valid @RequestBody PantryItemDtos.UpdateRequest body,
            HttpServletRequest request
    ) {
        return ApiResponse.success(service.update(id, body), requestId(request));
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "修改库存状态")
    public ApiResponse<PantryItemDtos.Response> updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody PantryItemDtos.StatusRequest body,
            HttpServletRequest request
    ) {
        return ApiResponse.success(service.updateStatus(id, body), requestId(request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除库存食材")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

    private String requestId(HttpServletRequest request) {
        Object requestId = request.getAttribute(RequestIdFilter.REQUEST_ID_ATTRIBUTE);
        return requestId == null ? "req_" + UUID.randomUUID() : requestId.toString();
    }
}
