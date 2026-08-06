package com.sccothe.fridgeclear.favorite.repository;

import com.sccothe.fridgeclear.favorite.domain.UserFavoriteRecipe;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface UserFavoriteRecipeRepository extends JpaRepository<UserFavoriteRecipe, Long> {
    Page<UserFavoriteRecipe> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    boolean existsByUserIdAndRecipeId(Long userId, Long recipeId);

    void deleteByUserIdAndRecipeId(Long userId, Long recipeId);

    @Query("SELECT f.recipeId FROM UserFavoriteRecipe f WHERE f.userId = :userId ORDER BY f.createdAt DESC")
    List<Long> findRecipeIdsByUserId(@Param("userId") Long userId);
}
