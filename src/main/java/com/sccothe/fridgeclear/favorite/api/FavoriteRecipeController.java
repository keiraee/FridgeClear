package com.sccothe.fridgeclear.favorite.api;

import com.sccothe.fridgeclear.common.api.ApiResponse;
import com.sccothe.fridgeclear.common.api.RequestIdFilter;
import com.sccothe.fridgeclear.favorite.service.FavoriteRecipeService;
import com.sccothe.fridgeclear.recipe.api.RecipeDtos;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/favorites")
@Tag(name = "菜谱收藏", description = "用户收藏菜谱管理")
@SecurityRequirement(name = "Authorization")
public class FavoriteRecipeController {
    private final FavoriteRecipeService service;

    public FavoriteRecipeController(FavoriteRecipeService service) {
        this.service = service;
    }

    @GetMapping("/ids")
    @Operation(summary = "获取当前用户收藏的菜谱 ID 列表")
    public ApiResponse<FavoriteDtos.IdListResponse> listIds(HttpServletRequest request) {
        return ApiResponse.success(service.listIds(), requestId(request));
    }

    @GetMapping
    @Operation(summary = "分页获取我的收藏菜谱")
    public ApiResponse<RecipeDtos.ListResponse> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            HttpServletRequest request
    ) {
        return ApiResponse.success(service.list(page, size), requestId(request));
    }

    @PostMapping("/{recipeId}")
    @Operation(summary = "收藏菜谱")
    public ApiResponse<Void> add(@PathVariable Long recipeId, HttpServletRequest request) {
        service.add(recipeId);
        return ApiResponse.success(null, requestId(request));
    }

    @DeleteMapping("/{recipeId}")
    @Operation(summary = "取消收藏菜谱")
    public ApiResponse<Void> remove(@PathVariable Long recipeId, HttpServletRequest request) {
        service.remove(recipeId);
        return ApiResponse.success(null, requestId(request));
    }

    private String requestId(HttpServletRequest request) {
        Object value = request.getAttribute(RequestIdFilter.REQUEST_ID_ATTRIBUTE);
        return value == null ? "req_" + UUID.randomUUID() : value.toString();
    }
}
