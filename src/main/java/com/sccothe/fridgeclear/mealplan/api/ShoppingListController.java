package com.sccothe.fridgeclear.mealplan.api;

import com.sccothe.fridgeclear.common.api.ApiResponse;
import com.sccothe.fridgeclear.common.api.RequestIdFilter;
import com.sccothe.fridgeclear.mealplan.service.MealPlanService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/shopping-list-items")
@Tag(name = "采购清单", description = "备餐计划采购清单管理")
public class ShoppingListController {
    private final MealPlanService service;

    public ShoppingListController(MealPlanService service) { this.service = service; }

    @PatchMapping("/{itemId}/status")
    @Operation(summary = "修改采购清单状态")
    public ApiResponse<MealPlanDtos.ShoppingResponse> updateStatus(
            @PathVariable Long itemId,
            @Valid @RequestBody MealPlanDtos.ShoppingStatusRequest body,
            HttpServletRequest request) {
        Object value = request.getAttribute(RequestIdFilter.REQUEST_ID_ATTRIBUTE);
        String requestId = value == null ? "req_" + UUID.randomUUID() : value.toString();
        return ApiResponse.success(service.updateShoppingStatus(itemId, body), requestId);
    }
}
