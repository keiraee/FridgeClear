package com.sccothe.fridgeclear.recipe.repository;

import com.sccothe.fridgeclear.recipe.domain.RecipeMedia;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface RecipeMediaQueryRepository extends JpaRepository<RecipeMedia, Long> {
    List<RecipeMedia> findByRecipeIdOrderBySortOrderAsc(Long recipeId);
}
