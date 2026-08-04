package com.sccothe.fridgeclear.recipe.api;

import com.sccothe.fridgeclear.recipe.domain.*;
import java.math.BigDecimal;
import java.util.List;

public final class RecipeDtos {
    private RecipeDtos() {}

    public record ListItem(Long id, String name, RecipeEnums.Category category, String description,
                           String difficultyText, Byte difficultyLevel, BigDecimal calories,
                           int ingredientCount, String coverImageUrl) {
        public static ListItem from(Recipe recipe, int ingredientCount, String coverImageUrl) {
            return new ListItem(recipe.getId(), recipe.getName(), recipe.getCategory(), recipe.getDescription(),
                    recipe.getDifficultyText(), recipe.getDifficultyLevel(), recipe.getCalories(), ingredientCount, coverImageUrl);
        }
    }

    public record ListResponse(List<ListItem> items, int page, int size, long total) {}

    public record IngredientItem(Long id, String name, RecipeEnums.IngredientRole role, boolean isOptional,
                                 String rawQuantity, BigDecimal quantityMin, BigDecimal quantityMax, String unit,
                                 RecipeEnums.QuantityParseStatus quantityParseStatus) {
        public static IngredientItem from(RecipeIngredient item) {
            return new IngredientItem(item.getId(), item.getRawName(), item.getRole(), item.isOptional(),
                    item.getRawQuantity(), item.getQuantityMin(), item.getQuantityMax(), item.getUnit(), item.getQuantityParseStatus());
        }
    }

    public record StepItem(Integer stepNo, String content) {
        public static StepItem from(RecipeStep step) { return new StepItem(step.getStepNo(), step.getContent()); }
    }

    public record MediaItem(RecipeEnums.MediaType mediaType, String sourcePath, String altText, Integer sortOrder) {
        public static MediaItem from(RecipeMedia media) { return new MediaItem(media.getMediaType(), media.getSourcePath(), media.getAltText(), media.getSortOrder()); }
    }

    public record SourceItem(String repository, String path, String commit) {
        public static SourceItem from(RecipeSourceDocument source) {
            return new SourceItem(source.getSourceRepository(), source.getSourcePath(), source.getSourceCommit());
        }
    }

    public record Detail(Long id, String name, RecipeEnums.Category category, String description,
                         String difficultyText, Byte difficultyLevel, BigDecimal calories,
                         List<IngredientItem> ingredients, List<StepItem> steps, List<MediaItem> media,
                         SourceItem source) {}
}
