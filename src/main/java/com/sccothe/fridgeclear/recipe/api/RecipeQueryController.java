package com.sccothe.fridgeclear.recipe.api;

import com.sccothe.fridgeclear.common.api.ApiResponse;
import com.sccothe.fridgeclear.common.api.RequestIdFilter;
import com.sccothe.fridgeclear.recipe.domain.RecipeEnums;
import com.sccothe.fridgeclear.recipe.service.RecipeQueryService;
import com.sccothe.fridgeclear.recipe.service.RecipeMediaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/recipes")
@Tag(name = "菜谱查询", description = "菜谱列表、详情和分类查询")
@SecurityRequirement(name = "Authorization")
public class RecipeQueryController {
    private final RecipeQueryService service;
    private final RecipeMediaService mediaService;

    public RecipeQueryController(RecipeQueryService service, RecipeMediaService mediaService) {
        this.service = service;
        this.mediaService = mediaService;
    }

    @GetMapping
    @Operation(summary = "分页查询菜谱")
    public ApiResponse<RecipeDtos.ListResponse> list(
            @Parameter(description = "菜名关键词") @RequestParam(required = false) String keyword,
            @RequestParam(required = false) RecipeEnums.Category category,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            HttpServletRequest request
    ) {
        return ApiResponse.success(service.list(keyword, category, page, size), requestId(request));
    }

    @GetMapping("/{id}")
    @Operation(summary = "查询菜谱详情")
    public ApiResponse<RecipeDtos.Detail> detail(@PathVariable Long id, HttpServletRequest request) {
        return ApiResponse.success(service.detail(id), requestId(request));
    }

    @GetMapping("/{id}/media/{sortOrder}")
    @Operation(summary = "获取菜谱图片")
    public ResponseEntity<Resource> media(@PathVariable Long id, @PathVariable Integer sortOrder) {
        RecipeMediaService.MediaFile media = mediaService.load(id, sortOrder);
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noCache())
                .contentType(media.mediaType())
                .body(media.resource());
    }

    private String requestId(HttpServletRequest request) {
        Object value = request.getAttribute(RequestIdFilter.REQUEST_ID_ATTRIBUTE);
        return value == null ? "req_" + UUID.randomUUID() : value.toString();
    }
}
