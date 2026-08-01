package com.sccothe.fridgeclear.recipe.repository;

import com.sccothe.fridgeclear.recipe.domain.RecipeStep;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RecipeStepRepository extends JpaRepository<RecipeStep, Long> {
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from RecipeStep item where item.recipeId = :recipeId")
    void deleteByRecipeId(@Param("recipeId") Long recipeId);
}
