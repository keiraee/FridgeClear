package com.sccothe.fridgeclear.recipe.repository;

import com.sccothe.fridgeclear.recipe.domain.RecipeMedia;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RecipeMediaRepository extends JpaRepository<RecipeMedia, Long> {
    void deleteByRecipeId(Long recipeId);
}
