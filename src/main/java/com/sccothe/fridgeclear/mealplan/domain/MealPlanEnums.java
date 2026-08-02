package com.sccothe.fridgeclear.mealplan.domain;

public final class MealPlanEnums {
    private MealPlanEnums() {
    }

    public enum PlanStatus { DRAFT, ACTIVE, COMPLETED, ARCHIVED }
    public enum MealType { BREAKFAST, LUNCH, DINNER, SNACK }
    public enum ItemStatus { PLANNED, COOKED, SKIPPED }
    public enum ShoppingStatus { TODO, PURCHASED, SKIPPED }
    public enum AiRunStatus { RUNNING, SUCCESS, FAILED }
}
