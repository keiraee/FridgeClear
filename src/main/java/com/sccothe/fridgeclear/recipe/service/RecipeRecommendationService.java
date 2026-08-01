package com.sccothe.fridgeclear.recipe.service;

import com.sccothe.fridgeclear.pantry.domain.PantryItem;
import com.sccothe.fridgeclear.pantry.domain.PantryItemStatus;
import com.sccothe.fridgeclear.pantry.repository.PantryItemRepository;
import com.sccothe.fridgeclear.recipe.api.RecommendationDtos;
import com.sccothe.fridgeclear.recipe.domain.IngredientAlias;
import com.sccothe.fridgeclear.recipe.domain.Recipe;
import com.sccothe.fridgeclear.recipe.domain.RecipeEnums;
import com.sccothe.fridgeclear.recipe.domain.RecipeIngredient;
import com.sccothe.fridgeclear.recipe.repository.IngredientAliasRepository;
import com.sccothe.fridgeclear.recipe.repository.IngredientRepository;
import com.sccothe.fridgeclear.recipe.repository.RecipeIngredientQueryRepository;
import com.sccothe.fridgeclear.recipe.repository.RecipeRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class RecipeRecommendationService {
    private static final long DEMO_USER_ID = 1L;

    private final PantryItemRepository pantryItemRepository;
    private final RecipeRepository recipeRepository;
    private final RecipeIngredientQueryRepository recipeIngredientRepository;
    private final IngredientRepository ingredientRepository;
    private final IngredientAliasRepository aliasRepository;

    public RecipeRecommendationService(PantryItemRepository pantryItemRepository,
                                       RecipeRepository recipeRepository,
                                       RecipeIngredientQueryRepository recipeIngredientRepository,
                                       IngredientRepository ingredientRepository,
                                       IngredientAliasRepository aliasRepository) {
        this.pantryItemRepository = pantryItemRepository;
        this.recipeRepository = recipeRepository;
        this.recipeIngredientRepository = recipeIngredientRepository;
        this.ingredientRepository = ingredientRepository;
        this.aliasRepository = aliasRepository;
    }

    public RecommendationDtos.RecipeMatchResponse recommend(int limit) {
        int safeLimit = Math.min(Math.max(limit, 1), 50);
        Map<String, Long> ingredientIdsByName = ingredientRepository.findAll().stream()
                .collect(Collectors.toMap(item -> item.getNormalizedName(), item -> item.getId(), (first, ignored) -> first));
        Map<String, Long> ingredientIdsByAlias = aliasRepository.findAll().stream()
                .collect(Collectors.toMap(IngredientAlias::getNormalizedAlias, IngredientAlias::getIngredientId, (first, ignored) -> first));
        List<PantryItem> pantryItems = pantryItemRepository.findByUserIdAndStatus(
                DEMO_USER_ID, PantryItemStatus.AVAILABLE,
                org.springframework.data.domain.PageRequest.of(0, 200,
                        Sort.by("expireDate").ascending().and(Sort.by("id").ascending()))).getContent();

        Set<Long> pantryIngredientIds = new HashSet<>();
        Set<String> pantryIngredientNames = new HashSet<>();
        for (PantryItem item : pantryItems) {
            if (item.getIngredientId() != null) {
                pantryIngredientIds.add(item.getIngredientId());
            }
            resolveIngredientId(item.getRawName(), ingredientIdsByName, ingredientIdsByAlias).ifPresent(pantryIngredientIds::add);
            pantryIngredientNames.add(IngredientNameNormalizer.normalize(item.getRawName()));
        }

        List<Recipe> recipes = recipeRepository.findByStatus(
                RecipeEnums.Status.ACTIVE,
                Sort.by("name").ascending());
        if (recipes.isEmpty()) {
            return new RecommendationDtos.RecipeMatchResponse(pantryIngredientIds.size(), List.of());
        }

        List<Long> recipeIds = recipes.stream().map(Recipe::getId).toList();
        Map<Long, List<RecipeIngredient>> ingredientsByRecipe = recipeIngredientRepository.findByRecipeIdIn(recipeIds)
                .stream().collect(Collectors.groupingBy(RecipeIngredient::getRecipeId));

        return new RecommendationDtos.RecipeMatchResponse(
                pantryIngredientIds.size(),
                recipes.stream()
                        .map(recipe -> match(recipe, ingredientsByRecipe.getOrDefault(recipe.getId(), List.of()), pantryIngredientIds, pantryIngredientNames))
                        .filter(Objects::nonNull)
                        .sorted(Comparator.comparingInt(RecommendationDtos.RecipeMatch::matchRate).reversed()
                                .thenComparingInt(RecommendationDtos.RecipeMatch::missingIngredientCount)
                                .thenComparing(RecommendationDtos.RecipeMatch::recipeName))
                        .limit(safeLimit)
                        .toList());
    }

    private RecommendationDtos.RecipeMatch match(Recipe recipe,
                                                  List<RecipeIngredient> allIngredients,
                                                  Set<Long> pantryIngredientIds,
                                                  Set<String> pantryIngredientNames) {
        Map<String, RecipeIngredient> uniqueRequired = new LinkedHashMap<>();
        allIngredients.stream()
                .filter(item -> item.getSourceSection() == RecipeEnums.SourceSection.REQUIRED)
                .filter(item -> !item.isOptional() && item.getRole() != RecipeEnums.IngredientRole.TOOL)
                .forEach(item -> uniqueRequired.putIfAbsent(ingredientKey(item), item));
        List<RecipeIngredient> required = new ArrayList<>(uniqueRequired.values());
        if (required.isEmpty()) {
            return null;
        }

        List<String> matched = new ArrayList<>();
        List<String> missing = new ArrayList<>();
        for (RecipeIngredient item : required) {
            String normalizedRawName = IngredientNameNormalizer.normalize(item.getRawName());
            boolean exists = (item.getIngredientId() != null && pantryIngredientIds.contains(item.getIngredientId()))
                    || pantryIngredientNames.contains(normalizedRawName);
            if (exists) {
                matched.add(item.getRawName());
            } else {
                missing.add(item.getRawName());
            }
        }
        if (matched.isEmpty()) {
            return null;
        }

        int requiredCount = required.size();
        int matchedCount = matched.size();
        int rate = matchedCount * 100 / requiredCount;
        return new RecommendationDtos.RecipeMatch(
                recipe.getId(), recipe.getName(), recipe.getCategory(), matchedCount, requiredCount,
                missing.size(), rate, matched.stream().distinct().toList(), missing.stream().distinct().toList());
    }

    private String ingredientKey(RecipeIngredient item) {
        return item.getIngredientId() == null
                ? "raw:" + IngredientNameNormalizer.normalize(item.getRawName())
                : "id:" + item.getIngredientId();
    }

    private Optional<Long> resolveIngredientId(String rawName,
                                               Map<String, Long> ingredientIdsByName,
                                               Map<String, Long> ingredientIdsByAlias) {
        String normalized = IngredientNameNormalizer.normalize(rawName);
        return Optional.ofNullable(ingredientIdsByName.get(normalized))
                .or(() -> Optional.ofNullable(ingredientIdsByAlias.get(normalized)));
    }
}
