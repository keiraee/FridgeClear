package com.sccothe.fridgeclear.mealplan.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "shopping_list_item")
public class ShoppingListItem {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(nullable = false) private Long mealPlanId;
    private Long ingredientId;
    @Column(nullable = false, length = 128) private String name;
    @Column(precision = 12, scale = 3) private BigDecimal quantity;
    @Column(length = 32) private String unit;
    @Column(length = 255) private String reason;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 32) private MealPlanEnums.ShoppingStatus status;

    public Long getId() { return id; }
    public Long getMealPlanId() { return mealPlanId; }
    public void setMealPlanId(Long value) { mealPlanId = value; }
    public Long getIngredientId() { return ingredientId; }
    public void setIngredientId(Long value) { ingredientId = value; }
    public String getName() { return name; }
    public void setName(String value) { name = value; }
    public BigDecimal getQuantity() { return quantity; }
    public void setQuantity(BigDecimal value) { quantity = value; }
    public String getUnit() { return unit; }
    public void setUnit(String value) { unit = value; }
    public String getReason() { return reason; }
    public void setReason(String value) { reason = value; }
    public MealPlanEnums.ShoppingStatus getStatus() { return status; }
    public void setStatus(MealPlanEnums.ShoppingStatus value) { status = value; }
}
