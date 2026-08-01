package com.sccothe.fridgeclear.recipe.repository;

import com.sccothe.fridgeclear.recipe.domain.Recipe;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface RecipeRepository extends JpaRepository<Recipe, Long> {
    Optional<Recipe> findBySourceDocumentId(Long sourceDocumentId);
}
