package com.sccothe.fridgeclear.mealplan.service;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
public class MealPlanGenerationWorker {
    private final MealPlanService mealPlanService;

    public MealPlanGenerationWorker(MealPlanService mealPlanService) {
        this.mealPlanService = mealPlanService;
    }

    @Async("mealPlanExecutor")
    public void executeAsync(Long runId) {
        mealPlanService.executeGenerate(runId);
    }
}
