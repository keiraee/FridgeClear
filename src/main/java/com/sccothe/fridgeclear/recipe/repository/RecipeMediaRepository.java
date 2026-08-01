package com.sccothe.fridgeclear.recipe.repository;

import com.sccothe.fridgeclear.recipe.domain.RecipeMedia;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RecipeMediaRepository extends JpaRepository<RecipeMedia, Long> {
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from RecipeMedia item where item.recipeId = :recipeId")
    void deleteByRecipeId(@Param("recipeId") Long recipeId);
}
