package com.sccothe.fridgeclear.recipe.repository;

import com.sccothe.fridgeclear.recipe.domain.Recipe;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import java.util.Optional;
import java.util.List;
import com.sccothe.fridgeclear.recipe.domain.RecipeEnums;

public interface RecipeRepository extends JpaRepository<Recipe, Long> {
    Optional<Recipe> findBySourceDocumentId(Long sourceDocumentId);
    Page<Recipe> findByStatus(RecipeEnums.Status status, Pageable pageable);
    List<Recipe> findByStatus(RecipeEnums.Status status, Sort sort);
    Page<Recipe> findByStatusAndCategory(RecipeEnums.Status status, RecipeEnums.Category category, Pageable pageable);
    Page<Recipe> findByStatusAndNameContaining(RecipeEnums.Status status, String name, Pageable pageable);
    Page<Recipe> findByStatusAndCategoryAndNameContaining(RecipeEnums.Status status, RecipeEnums.Category category, String name, Pageable pageable);

    @Query("""
            SELECT DISTINCT r FROM Recipe r
            LEFT JOIN RecipeIngredient ri ON ri.recipeId = r.id
            WHERE r.status = :status
              AND (:category IS NULL OR r.category = :category)
              AND (
                r.name LIKE CONCAT('%', :keyword, '%')
                OR COALESCE(r.description, '') LIKE CONCAT('%', :keyword, '%')
                OR ri.rawName LIKE CONCAT('%', :keyword, '%')
              )
            """)
    Page<Recipe> searchByKeyword(
            @Param("status") RecipeEnums.Status status,
            @Param("category") RecipeEnums.Category category,
            @Param("keyword") String keyword,
            Pageable pageable);
}
