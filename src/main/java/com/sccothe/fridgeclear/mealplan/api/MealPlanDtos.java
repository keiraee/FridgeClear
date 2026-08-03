package com.sccothe.fridgeclear.mealplan.api;

import com.sccothe.fridgeclear.mealplan.domain.MealPlanEnums;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public final class MealPlanDtos {
    private MealPlanDtos() {}

    @Schema(name = "MealPlanGenerateRequest", description = "AI 备餐计划生成请求")
    public record GenerateRequest(
            @Min(1) @Max(7) Integer days,
            @Min(1) @Max(10) Integer peopleCount,
            @Min(10) @Max(180) Integer maxCookingMinutes,
            @NotEmpty List<MealPlanEnums.MealType> mealTypes,
            String dietaryPreference,
            List<String> dislikedIngredients,
            List<String> availableAppliances,
            List<Long> usePantryItemIds
    ) {}

    public record Response(Long mealPlanId, String summary, List<ExpiringIngredient> expiringIngredients,
                           List<ItemResponse> items, List<ShoppingResponse> shoppingList) {}
    public record ListItem(Long id, String title, LocalDate startDate, LocalDate endDate, MealPlanEnums.PlanStatus status) {}
    public record PageResponse(List<ListItem> content, int page, int size, long totalElements, int totalPages) {}
    public record DetailResponse(Long id, String title, LocalDate startDate, LocalDate endDate,
                                 MealPlanEnums.PlanStatus status, List<ItemResponse> items,
                                 List<ShoppingResponse> shoppingList) {}
    @Schema(name = "MealPlanItemStatusRequest", description = "修改计划项状态请求")
    public record ItemStatusRequest(@jakarta.validation.constraints.NotNull MealPlanEnums.ItemStatus status) {}
    @Schema(name = "ShoppingStatusRequest", description = "修改采购项状态请求")
    public record ShoppingStatusRequest(@jakarta.validation.constraints.NotNull MealPlanEnums.ShoppingStatus status) {}
    public record ExpiringIngredient(Long pantryItemId, String name, LocalDate expireDate, String reason) {}
    public record ItemResponse(Long id, LocalDate planDate, MealPlanEnums.MealType mealType,
                               RecipeResponse recipe, BigDecimal servings, List<String> usedIngredients,
                               List<String> missingIngredients, String reason, MealPlanEnums.ItemStatus status) {}
    public record RecipeResponse(Long id, String name) {}
    public record ShoppingResponse(Long id, String name, BigDecimal quantity, String unit, String reason,
                                   MealPlanEnums.ShoppingStatus status) {}
}
