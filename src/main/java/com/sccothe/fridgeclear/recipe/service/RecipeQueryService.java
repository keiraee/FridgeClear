package com.sccothe.fridgeclear.recipe.service;

import com.sccothe.fridgeclear.common.api.ResourceNotFoundException;
import com.sccothe.fridgeclear.recipe.api.RecipeDtos;
import com.sccothe.fridgeclear.recipe.domain.Recipe;
import com.sccothe.fridgeclear.recipe.domain.RecipeEnums;
import com.sccothe.fridgeclear.recipe.repository.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class RecipeQueryService {
    private final RecipeRepository recipeRepository;
    private final RecipeSourceDocumentRepository sourceRepository;
    private final RecipeIngredientQueryRepository ingredientRepository;
    private final RecipeStepQueryRepository stepRepository;
    private final RecipeMediaQueryRepository mediaRepository;

    public RecipeQueryService(RecipeRepository recipeRepository,
                              RecipeSourceDocumentRepository sourceRepository,
                              RecipeIngredientQueryRepository ingredientRepository,
                              RecipeStepQueryRepository stepRepository,
                              RecipeMediaQueryRepository mediaRepository) {
        this.recipeRepository = recipeRepository;
        this.sourceRepository = sourceRepository;
        this.ingredientRepository = ingredientRepository;
        this.stepRepository = stepRepository;
        this.mediaRepository = mediaRepository;
    }

    public RecipeDtos.ListResponse list(String keyword, RecipeEnums.Category category, int page, int size) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 100);
        var pageable = PageRequest.of(safePage, safeSize, Sort.by("name").ascending());
        String safeKeyword = keyword == null ? "" : keyword.trim();
        Page<Recipe> result;
        if (category != null && !safeKeyword.isBlank()) {
            result = recipeRepository.findByStatusAndCategoryAndNameContaining(RecipeEnums.Status.ACTIVE, category, safeKeyword, pageable);
        } else if (category != null) {
            result = recipeRepository.findByStatusAndCategory(RecipeEnums.Status.ACTIVE, category, pageable);
        } else if (!safeKeyword.isBlank()) {
            result = recipeRepository.findByStatusAndNameContaining(RecipeEnums.Status.ACTIVE, safeKeyword, pageable);
        } else {
            result = recipeRepository.findByStatus(RecipeEnums.Status.ACTIVE, pageable);
        }
        return new RecipeDtos.ListResponse(
                result.getContent().stream().map(recipe -> RecipeDtos.ListItem.from(
                        recipe,
                        (int) ingredientRepository.countByRecipeId(recipe.getId()),
                        mediaRepository.findByRecipeIdOrderBySortOrderAsc(recipe.getId()).stream()
                                .findFirst()
                                .map(media -> "/api/v1/recipes/" + recipe.getId() + "/media/" + media.getSortOrder())
                                .orElse(null)
                )).toList(),
                result.getNumber(), result.getSize(), result.getTotalElements());
    }

    public RecipeDtos.Detail detail(Long id) {
        Recipe recipe = recipeRepository.findById(id)
                .filter(item -> item.getStatus() == RecipeEnums.Status.ACTIVE)
                .orElseThrow(() -> new ResourceNotFoundException("菜谱不存在: " + id));
        var source = sourceRepository.findById(recipe.getSourceDocumentId())
                .map(RecipeDtos.SourceItem::from)
                .orElse(null);
        return new RecipeDtos.Detail(
                recipe.getId(), recipe.getName(), recipe.getCategory(), recipe.getDescription(),
                recipe.getDifficultyText(), recipe.getDifficultyLevel(), recipe.getCalories(),
                ingredientRepository.findByRecipeIdOrderBySortOrderAsc(id).stream().map(RecipeDtos.IngredientItem::from).toList(),
                stepRepository.findByRecipeIdOrderByStepNoAsc(id).stream().map(RecipeDtos.StepItem::from).toList(),
                mediaRepository.findByRecipeIdOrderBySortOrderAsc(id).stream().map(RecipeDtos.MediaItem::from).toList(),
                source
        );
    }
}
