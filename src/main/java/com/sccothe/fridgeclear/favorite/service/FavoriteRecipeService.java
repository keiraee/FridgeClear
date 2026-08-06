package com.sccothe.fridgeclear.favorite.service;

import com.sccothe.fridgeclear.auth.service.CurrentUser;
import com.sccothe.fridgeclear.common.api.ResourceNotFoundException;
import com.sccothe.fridgeclear.favorite.api.FavoriteDtos;
import com.sccothe.fridgeclear.favorite.domain.UserFavoriteRecipe;
import com.sccothe.fridgeclear.favorite.repository.UserFavoriteRecipeRepository;
import com.sccothe.fridgeclear.recipe.api.RecipeDtos;
import com.sccothe.fridgeclear.recipe.domain.Recipe;
import com.sccothe.fridgeclear.recipe.domain.RecipeEnums;
import com.sccothe.fridgeclear.recipe.repository.RecipeIngredientQueryRepository;
import com.sccothe.fridgeclear.recipe.repository.RecipeMediaQueryRepository;
import com.sccothe.fridgeclear.recipe.repository.RecipeRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@Transactional
public class FavoriteRecipeService {
    private final UserFavoriteRecipeRepository favoriteRepository;
    private final RecipeRepository recipeRepository;
    private final RecipeIngredientQueryRepository ingredientRepository;
    private final RecipeMediaQueryRepository mediaRepository;

    public FavoriteRecipeService(UserFavoriteRecipeRepository favoriteRepository,
                                 RecipeRepository recipeRepository,
                                 RecipeIngredientQueryRepository ingredientRepository,
                                 RecipeMediaQueryRepository mediaRepository) {
        this.favoriteRepository = favoriteRepository;
        this.recipeRepository = recipeRepository;
        this.ingredientRepository = ingredientRepository;
        this.mediaRepository = mediaRepository;
    }

    @Transactional(readOnly = true)
    public FavoriteDtos.IdListResponse listIds() {
        return new FavoriteDtos.IdListResponse(favoriteRepository.findRecipeIdsByUserId(CurrentUser.id()));
    }

    @Transactional(readOnly = true)
    public RecipeDtos.ListResponse list(int page, int size) {
        Long userId = CurrentUser.id();
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 100);
        Pageable pageable = PageRequest.of(safePage, safeSize);
        Page<UserFavoriteRecipe> favorites = favoriteRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
        List<Long> recipeIds = favorites.getContent().stream().map(UserFavoriteRecipe::getRecipeId).toList();
        if (recipeIds.isEmpty()) {
            return new RecipeDtos.ListResponse(List.of(), favorites.getNumber(), favorites.getSize(), favorites.getTotalElements());
        }

        Map<Long, Recipe> recipesById = recipeRepository.findAllById(recipeIds).stream()
                .filter(recipe -> recipe.getStatus() == RecipeEnums.Status.ACTIVE)
                .collect(Collectors.toMap(Recipe::getId, recipe -> recipe));
        List<Recipe> orderedRecipes = recipeIds.stream()
                .map(recipesById::get)
                .filter(Objects::nonNull)
                .toList();

        Map<Long, Integer> ingredientCounts = loadIngredientCounts(orderedRecipes.stream().map(Recipe::getId).toList());
        Map<Long, String> coverImageUrls = loadCoverImageUrls(orderedRecipes.stream().map(Recipe::getId).toList());
        return new RecipeDtos.ListResponse(
                orderedRecipes.stream().map(recipe -> RecipeDtos.ListItem.from(
                        recipe,
                        ingredientCounts.getOrDefault(recipe.getId(), 0),
                        coverImageUrls.get(recipe.getId())
                )).toList(),
                favorites.getNumber(),
                favorites.getSize(),
                favorites.getTotalElements()
        );
    }

    public void add(Long recipeId) {
        Long userId = CurrentUser.id();
        Recipe recipe = recipeRepository.findById(recipeId)
                .filter(item -> item.getStatus() == RecipeEnums.Status.ACTIVE)
                .orElseThrow(() -> new ResourceNotFoundException("菜谱不存在: " + recipeId));
        if (favoriteRepository.existsByUserIdAndRecipeId(userId, recipe.getId())) {
            return;
        }
        UserFavoriteRecipe favorite = new UserFavoriteRecipe();
        favorite.setUserId(userId);
        favorite.setRecipeId(recipe.getId());
        try {
            favoriteRepository.save(favorite);
        } catch (DataIntegrityViolationException ignored) {
            // 并发重复收藏时唯一键冲突，按幂等成功处理
        }
    }

    public void remove(Long recipeId) {
        favoriteRepository.deleteByUserIdAndRecipeId(CurrentUser.id(), recipeId);
    }

    private Map<Long, Integer> loadIngredientCounts(List<Long> recipeIds) {
        if (recipeIds.isEmpty()) return Map.of();
        return ingredientRepository.findByRecipeIdIn(recipeIds).stream()
                .collect(Collectors.groupingBy(
                        com.sccothe.fridgeclear.recipe.domain.RecipeIngredient::getRecipeId,
                        Collectors.collectingAndThen(Collectors.counting(), Long::intValue)));
    }

    private Map<Long, String> loadCoverImageUrls(List<Long> recipeIds) {
        if (recipeIds.isEmpty()) return Map.of();
        Map<Long, String> coverImageUrls = new HashMap<>();
        for (var media : mediaRepository.findByRecipeIdInOrderByRecipeIdAscSortOrderAsc(recipeIds)) {
            coverImageUrls.putIfAbsent(media.getRecipeId(),
                    "/api/v1/recipes/" + media.getRecipeId() + "/media/" + media.getSortOrder());
        }
        return coverImageUrls;
    }
}
