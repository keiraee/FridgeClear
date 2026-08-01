package com.sccothe.fridgeclear.recipe.repository;

import com.sccothe.fridgeclear.recipe.domain.RecipeIngredient;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RecipeIngredientRepository extends JpaRepository<RecipeIngredient, Long> {
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from RecipeIngredient item where item.recipeId = :recipeId")
    void deleteByRecipeId(@Param("recipeId") Long recipeId);
}
