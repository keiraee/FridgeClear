package com.sccothe.fridgeclear.mealplan.api;

import com.sccothe.fridgeclear.common.api.ApiResponse;
import com.sccothe.fridgeclear.common.api.RequestIdFilter;
import com.sccothe.fridgeclear.mealplan.service.MealPlanService;
import com.sccothe.fridgeclear.mealplan.domain.MealPlanEnums;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/meal-plans")
@Tag(name = "AI 备餐计划", description = "根据库存和菜谱生成 AI 备餐计划")
@SecurityRequirement(name = "Authorization")
public class MealPlanController {
    private final MealPlanService service;

    public MealPlanController(MealPlanService service) { this.service = service; }

    @PostMapping("/generate")
    @Operation(summary = "提交 AI 备餐计划生成任务")
    public ResponseEntity<ApiResponse<MealPlanDtos.SubmitResponse>> generate(
            @Valid @RequestBody MealPlanDtos.GenerateRequest request,
            HttpServletRequest httpRequest
    ) {
        Long taskId = service.submitGenerate(request);
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(ApiResponse.success(new MealPlanDtos.SubmitResponse(taskId), requestId(httpRequest)));
    }

    @GetMapping("/generate/tasks/{taskId}")
    @Operation(summary = "查询 AI 备餐计划生成任务状态")
    public ApiResponse<MealPlanDtos.TaskStatusResponse> taskStatus(
            @PathVariable Long taskId,
            HttpServletRequest httpRequest
    ) {
        return ApiResponse.success(service.getTaskStatus(taskId), requestId(httpRequest));
    }

    @GetMapping
    @Operation(summary = "查询备餐计划列表")
    public ApiResponse<MealPlanDtos.PageResponse> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) MealPlanEnums.PlanStatus status,
            HttpServletRequest request) {
        return success(service.list(page, size, status), request);
    }

    @GetMapping("/{id}")
    @Operation(summary = "查询备餐计划详情")
    public ApiResponse<MealPlanDtos.DetailResponse> detail(@PathVariable Long id, HttpServletRequest request) {
        return success(service.detail(id), request);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "归档备餐计划")
    public ApiResponse<Void> archive(@PathVariable Long id, HttpServletRequest request) {
        service.archive(id);
        return success(null, request);
    }

    @PatchMapping("/{planId}/items/{itemId}/status")
    @Operation(summary = "修改备餐计划项状态")
    public ApiResponse<MealPlanDtos.ItemResponse> updateItemStatus(
            @PathVariable Long planId, @PathVariable Long itemId,
            @Valid @RequestBody MealPlanDtos.ItemStatusRequest body, HttpServletRequest request) {
        return success(service.updateItemStatus(planId, itemId, body), request);
    }

    @GetMapping("/{planId}/shopping-list")
    @Operation(summary = "获取备餐计划采购清单")
    public ApiResponse<java.util.List<MealPlanDtos.ShoppingResponse>> shoppingList(@PathVariable Long planId, HttpServletRequest request) {
        return success(service.shoppingList(planId), request);
    }

    private <T> ApiResponse<T> success(T data, HttpServletRequest request) {
        return ApiResponse.success(data, requestId(request));
    }

    private String requestId(HttpServletRequest request) {
        Object value = request.getAttribute(RequestIdFilter.REQUEST_ID_ATTRIBUTE);
        return value == null ? "req_" + UUID.randomUUID() : value.toString();
    }
}
