package com.sccothe.fridgeclear.recipe.repository;

import com.sccothe.fridgeclear.recipe.domain.IngredientAlias;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface IngredientAliasRepository extends JpaRepository<IngredientAlias, Long> {
    Optional<IngredientAlias> findByNormalizedAlias(String normalizedAlias);
    boolean existsByNormalizedAlias(String normalizedAlias);
}
