package com.sccothe.fridgeclear.recipe.domain;

import jakarta.persistence.*;

@Entity
@Table(name = "recipe_step", uniqueConstraints = @UniqueConstraint(name = "uk_recipe_step", columnNames = {"recipe_id", "step_no"}))
public class RecipeStep {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(nullable = false) private Long recipeId;
    @Column(nullable = false) private Integer stepNo;
    @Lob @Column(nullable = false, columnDefinition = "longtext") private String content;
    public Long getId() { return id; }
    public void setRecipeId(Long v) { recipeId = v; }
    public void setStepNo(Integer v) { stepNo = v; }
    public void setContent(String v) { content = v; }
}
