package com.sccothe.fridgeclear.recipe.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "ingredient", uniqueConstraints = @UniqueConstraint(name = "uk_ingredient_normalized_name", columnNames = "normalized_name"))
public class Ingredient {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(nullable = false, length = 128) private String canonicalName;
    @Column(nullable = false, length = 128) private String normalizedName;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 32) private RecipeEnums.IngredientType ingredientType;
    @Column(length = 32) private String defaultUnit;
    @Column(nullable = false) private LocalDateTime createdAt;
    @Column(nullable = false) private LocalDateTime updatedAt;
    @PrePersist void onCreate() { var now = LocalDateTime.now(); createdAt = now; updatedAt = now; }
    @PreUpdate void onUpdate() { updatedAt = LocalDateTime.now(); }
    public Long getId() { return id; }
    public String getCanonicalName() { return canonicalName; }
    public void setCanonicalName(String v) { canonicalName = v; }
    public String getNormalizedName() { return normalizedName; }
    public void setNormalizedName(String v) { normalizedName = v; }
    public RecipeEnums.IngredientType getIngredientType() { return ingredientType; }
    public void setIngredientType(RecipeEnums.IngredientType v) { ingredientType = v; }
    public String getDefaultUnit() { return defaultUnit; }
    public void setDefaultUnit(String v) { defaultUnit = v; }
}
