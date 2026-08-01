package com.sccothe.fridgeclear.recipe.repository;

import com.sccothe.fridgeclear.recipe.domain.RecipeStep;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface RecipeStepQueryRepository extends JpaRepository<RecipeStep, Long> {
    List<RecipeStep> findByRecipeIdOrderByStepNoAsc(Long recipeId);
}
