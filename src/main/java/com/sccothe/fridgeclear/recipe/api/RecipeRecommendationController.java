package com.sccothe.fridgeclear.recipe.api;

import com.sccothe.fridgeclear.common.api.ApiResponse;
import com.sccothe.fridgeclear.common.api.RequestIdFilter;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/recommendations")
@Tag(name = "Recommendation", description = "基于库存的菜谱推荐")
public class RecipeRecommendationController {
    private final com.sccothe.fridgeclear.recipe.service.RecipeRecommendationService service;

    public RecipeRecommendationController(com.sccothe.fridgeclear.recipe.service.RecipeRecommendationService service) {
        this.service = service;
    }

    @GetMapping("/recipes")
    @Operation(summary = "根据库存推荐菜谱")
    public ApiResponse<RecommendationDtos.RecipeMatchResponse> recipes(
            @Parameter(description = "最多返回数量，范围 1-50") @RequestParam(defaultValue = "20") int limit,
            HttpServletRequest request) {
        Object value = request.getAttribute(RequestIdFilter.REQUEST_ID_ATTRIBUTE);
        String requestId = value == null ? "req_" + UUID.randomUUID() : value.toString();
        return ApiResponse.success(service.recommend(limit), requestId);
    }
}
