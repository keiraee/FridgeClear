package com.sccothe.fridgeclear.recipe.api;

import com.sccothe.fridgeclear.common.api.ApiResponse;
import com.sccothe.fridgeclear.recipe.service.IngredientNormalizationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/ingredients")
@Tag(name = "食材标准化", description = "食材名称和别名标准化管理")
public class IngredientNormalizationController {
    private final IngredientNormalizationService service;

    public IngredientNormalizationController(IngredientNormalizationService service) { this.service = service; }

    @PostMapping("/normalize")
    @Operation(summary = "标准化食材名称")
    public ApiResponse<IngredientNormalizationService.NormalizeReport> normalize() {
        return ApiResponse.success(service.normalizeAll(), "normalize");
    }
}
