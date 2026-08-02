package com.sccothe.fridgeclear.mealplan.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "meal_plan_item")
public class MealPlanItem {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(nullable = false) private Long mealPlanId;
    @Column(nullable = false) private LocalDate planDate;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 32) private MealPlanEnums.MealType mealType;
    @Column(nullable = false) private Long recipeId;
    @Column(precision = 8, scale = 2) private BigDecimal servings;
    @Lob @Column(columnDefinition = "longtext") private String usedIngredientsJson;
    @Lob @Column(columnDefinition = "longtext") private String missingIngredientsJson;
    @Lob @Column(columnDefinition = "longtext") private String reason;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 32) private MealPlanEnums.ItemStatus status;
    @Column(nullable = false) private Integer sortOrder;

    public Long getId() { return id; }
    public Long getMealPlanId() { return mealPlanId; }
    public void setMealPlanId(Long value) { mealPlanId = value; }
    public LocalDate getPlanDate() { return planDate; }
    public void setPlanDate(LocalDate value) { planDate = value; }
    public MealPlanEnums.MealType getMealType() { return mealType; }
    public void setMealType(MealPlanEnums.MealType value) { mealType = value; }
    public Long getRecipeId() { return recipeId; }
    public void setRecipeId(Long value) { recipeId = value; }
    public BigDecimal getServings() { return servings; }
    public void setServings(BigDecimal value) { servings = value; }
    public String getUsedIngredientsJson() { return usedIngredientsJson; }
    public void setUsedIngredientsJson(String value) { usedIngredientsJson = value; }
    public String getMissingIngredientsJson() { return missingIngredientsJson; }
    public void setMissingIngredientsJson(String value) { missingIngredientsJson = value; }
    public String getReason() { return reason; }
    public void setReason(String value) { reason = value; }
    public MealPlanEnums.ItemStatus getStatus() { return status; }
    public void setStatus(MealPlanEnums.ItemStatus value) { status = value; }
    public Integer getSortOrder() { return sortOrder; }
    public void setSortOrder(Integer value) { sortOrder = value; }
}
