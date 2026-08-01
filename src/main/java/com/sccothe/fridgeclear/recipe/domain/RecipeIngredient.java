package com.sccothe.fridgeclear.recipe.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "recipe_ingredient")
public class RecipeIngredient {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(nullable = false) private Long recipeId;
    private Long ingredientId;
    @Column(nullable = false, length = 255) private String rawName;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 32) private RecipeEnums.IngredientRole role;
    @Column(name = "is_optional", nullable = false) private boolean optional;
    @Column(length = 255) private String rawQuantity;
    @Column(precision = 12, scale = 3) private BigDecimal quantityMin;
    @Column(precision = 12, scale = 3) private BigDecimal quantityMax;
    @Column(length = 32) private String unit;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 32) private RecipeEnums.QuantityParseStatus quantityParseStatus;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 32) private RecipeEnums.SourceSection sourceSection;
    @Column(nullable = false) private Integer sortOrder;
    public Long getId() { return id; }
    public Long getRecipeId() { return recipeId; }
    public void setRecipeId(Long v) { recipeId = v; }
    public Long getIngredientId() { return ingredientId; }
    public void setIngredientId(Long v) { ingredientId = v; }
    public String getRawName() { return rawName; }
    public void setRawName(String v) { rawName = v; }
    public RecipeEnums.IngredientRole getRole() { return role; }
    public void setRole(RecipeEnums.IngredientRole v) { role = v; }
    public boolean isOptional() { return optional; }
    public void setOptional(boolean v) { optional = v; }
    public String getRawQuantity() { return rawQuantity; }
    public void setRawQuantity(String v) { rawQuantity = v; }
    public BigDecimal getQuantityMin() { return quantityMin; }
    public void setQuantityMin(BigDecimal v) { quantityMin = v; }
    public BigDecimal getQuantityMax() { return quantityMax; }
    public void setQuantityMax(BigDecimal v) { quantityMax = v; }
    public String getUnit() { return unit; }
    public void setUnit(String v) { unit = v; }
    public RecipeEnums.QuantityParseStatus getQuantityParseStatus() { return quantityParseStatus; }
    public void setQuantityParseStatus(RecipeEnums.QuantityParseStatus v) { quantityParseStatus = v; }
    public RecipeEnums.SourceSection getSourceSection() { return sourceSection; }
    public void setSourceSection(RecipeEnums.SourceSection v) { sourceSection = v; }
    public Integer getSortOrder() { return sortOrder; }
    public void setSortOrder(Integer v) { sortOrder = v; }
}
