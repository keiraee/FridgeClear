package com.sccothe.fridgeclear.recipe.api;

import com.sccothe.fridgeclear.recipe.domain.RecipeEnums;

import java.util.List;

public final class RecommendationDtos {
    private RecommendationDtos() {
    }

    public record RecipeMatch(
            Long recipeId,
            String recipeName,
            RecipeEnums.Category category,
            int matchedIngredientCount,
            int requiredIngredientCount,
            int missingIngredientCount,
            int matchRate,
            List<String> matchedIngredients,
            List<String> missingIngredients
    ) {
    }

    public record RecipeMatchResponse(
            int pantryIngredientCount,
            List<RecipeMatch> recipes
    ) {
    }
}
