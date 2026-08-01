package com.sccothe.fridgeclear.recipe.repository;

import com.sccothe.fridgeclear.recipe.domain.RecipeIngredient;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface RecipeIngredientQueryRepository extends JpaRepository<RecipeIngredient, Long> {
    List<RecipeIngredient> findByRecipeIdOrderBySortOrderAsc(Long recipeId);
    List<RecipeIngredient> findByRecipeIdIn(List<Long> recipeIds);
    long countByRecipeId(Long recipeId);
}
