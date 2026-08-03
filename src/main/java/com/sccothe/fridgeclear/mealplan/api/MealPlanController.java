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
@RequestMapping("/api/v1/meal-plans")
@Tag(name = "AI 备餐计划", description = "根据库存和菜谱生成 AI 备餐计划")
public class MealPlanController {
    private final MealPlanService service;

    public MealPlanController(MealPlanService service) { this.service = service; }

    @PostMapping("/generate")
    @Operation(summary = "生成 AI 备餐计划")
    public ApiResponse<MealPlanDtos.Response> generate(@Valid @RequestBody MealPlanDtos.GenerateRequest request, HttpServletRequest httpRequest) {
        Object value = httpRequest.getAttribute(RequestIdFilter.REQUEST_ID_ATTRIBUTE);
        String requestId = value == null ? "req_" + UUID.randomUUID() : value.toString();
        return ApiResponse.success(service.generate(request), requestId);
    }
}
