package com.sccothe.fridgeclear.recipe.repository;

import com.sccothe.fridgeclear.recipe.domain.RecipeSourceDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface RecipeSourceDocumentRepository extends JpaRepository<RecipeSourceDocument, Long> {
    Optional<RecipeSourceDocument> findBySourceRepositoryAndSourcePath(String sourceRepository, String sourcePath);
}
