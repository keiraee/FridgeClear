package com.sccothe.fridgeclear.recipe.service;

import com.sccothe.fridgeclear.auth.service.CurrentUser;
import com.sccothe.fridgeclear.pantry.domain.PantryItem;
import com.sccothe.fridgeclear.pantry.domain.PantryItemStatus;
import com.sccothe.fridgeclear.pantry.repository.PantryItemRepository;
import com.sccothe.fridgeclear.recipe.api.RecommendationDtos;
import com.sccothe.fridgeclear.recipe.api.RecommendationFilter;
import com.sccothe.fridgeclear.recipe.domain.IngredientAlias;
import com.sccothe.fridgeclear.recipe.domain.Recipe;
import com.sccothe.fridgeclear.recipe.domain.RecipeEnums;
import com.sccothe.fridgeclear.recipe.domain.RecipeIngredient;
import com.sccothe.fridgeclear.recipe.repository.IngredientAliasRepository;
import com.sccothe.fridgeclear.recipe.repository.IngredientRepository;
import com.sccothe.fridgeclear.recipe.repository.RecipeIngredientQueryRepository;
import com.sccothe.fridgeclear.recipe.repository.RecipeMediaQueryRepository;
import com.sccothe.fridgeclear.recipe.repository.RecipeRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class RecipeRecommendationService {
    private static final int EXPIRING_WITHIN_DAYS = 3;
    /** 「可尝试」最低匹配率：避免只命中 1 个食材的低相关菜谱 */
    private static final int ALL_MIN_MATCH_RATE = 40;
    private static final int HIGH_MATCH_MIN_RATE = 60;
    private static final int READY_NOW_MAX_MISSING = 2;

    private final PantryItemRepository pantryItemRepository;
    private final RecipeRepository recipeRepository;
    private final RecipeIngredientQueryRepository recipeIngredientRepository;
    private final IngredientRepository ingredientRepository;
    private final IngredientAliasRepository aliasRepository;
    private final RecipeMediaQueryRepository mediaRepository;

    public RecipeRecommendationService(PantryItemRepository pantryItemRepository,
                                       RecipeRepository recipeRepository,
                                       RecipeIngredientQueryRepository recipeIngredientRepository,
                                       IngredientRepository ingredientRepository,
                                       IngredientAliasRepository aliasRepository,
                                       RecipeMediaQueryRepository mediaRepository) {
        this.pantryItemRepository = pantryItemRepository;
        this.recipeRepository = recipeRepository;
        this.recipeIngredientRepository = recipeIngredientRepository;
        this.ingredientRepository = ingredientRepository;
        this.aliasRepository = aliasRepository;
        this.mediaRepository = mediaRepository;
    }

    public RecommendationDtos.RecipeMatchResponse recommend(int limit, RecommendationFilter filter) {
        int safeLimit = Math.min(Math.max(limit, 1), 50);
        Long userId = CurrentUser.id();
        LocalDate today = LocalDate.now();

        Map<String, Long> ingredientIdsByName = ingredientRepository.findAll().stream()
                .collect(Collectors.toMap(item -> item.getNormalizedName(), item -> item.getId(), (first, ignored) -> first));
        Map<String, Long> ingredientIdsByAlias = aliasRepository.findAll().stream()
                .collect(Collectors.toMap(IngredientAlias::getNormalizedAlias, IngredientAlias::getIngredientId, (first, ignored) -> first));
        Map<Long, Set<String>> namesByIngredientId = buildNamesByIngredientId(ingredientIdsByName, aliasRepository.findAll());

        List<PantryItem> pantryItems = pantryItemRepository.findByUserIdAndStatus(
                userId, PantryItemStatus.AVAILABLE,
                org.springframework.data.domain.PageRequest.of(0, 200,
                        Sort.by("expireDate").ascending().and(Sort.by("id").ascending()))).getContent();

        PantryContext pantry = buildPantryContext(pantryItems, today, ingredientIdsByName, ingredientIdsByAlias, namesByIngredientId);

        List<Recipe> recipes = recipeRepository.findByStatus(
                RecipeEnums.Status.ACTIVE,
                Sort.by("name").ascending());
        if (recipes.isEmpty()) {
            return new RecommendationDtos.RecipeMatchResponse(pantry.ingredientIds.size(), List.of());
        }

        List<Long> recipeIds = recipes.stream().map(Recipe::getId).toList();
        Map<Long, List<RecipeIngredient>> ingredientsByRecipe = recipeIngredientRepository.findByRecipeIdIn(recipeIds)
                .stream().collect(Collectors.groupingBy(RecipeIngredient::getRecipeId));
        Map<Long, String> coverImageUrls = loadCoverImageUrls(recipeIds);
        Map<Long, Integer> ingredientCounts = ingredientsByRecipe.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue().size()));

        List<RecommendationDtos.RecipeMatch> matches = recipes.stream()
                .map(recipe -> match(recipe, ingredientsByRecipe.getOrDefault(recipe.getId(), List.of()),
                        pantry, ingredientIdsByName, ingredientIdsByAlias,
                        coverImageUrls.get(recipe.getId()),
                        ingredientCounts.getOrDefault(recipe.getId(), 0)))
                .filter(Objects::nonNull)
                .filter(item -> passesFilter(item, filter))
                .sorted(Comparator.comparingInt(RecommendationDtos.RecipeMatch::matchRate).reversed()
                        .thenComparingInt(RecommendationDtos.RecipeMatch::expiringMatchedCount).reversed()
                        .thenComparingInt(RecommendationDtos.RecipeMatch::missingIngredientCount)
                        .thenComparing(RecommendationDtos.RecipeMatch::recipeName))
                .limit(safeLimit)
                .toList();

        return new RecommendationDtos.RecipeMatchResponse(pantry.ingredientIds.size(), matches);
    }

    private boolean passesFilter(RecommendationDtos.RecipeMatch match, RecommendationFilter filter) {
        return switch (filter) {
            case ALL -> match.matchRate() >= ALL_MIN_MATCH_RATE;
            case HIGH_MATCH -> match.matchRate() >= HIGH_MATCH_MIN_RATE;
            case READY_NOW -> match.missingIngredientCount() <= READY_NOW_MAX_MISSING;
        };
    }

    private PantryContext buildPantryContext(List<PantryItem> pantryItems,
                                             LocalDate today,
                                             Map<String, Long> ingredientIdsByName,
                                             Map<String, Long> ingredientIdsByAlias,
                                             Map<Long, Set<String>> namesByIngredientId) {
        Set<Long> ingredientIds = new HashSet<>();
        Set<String> ingredientNames = new HashSet<>();
        Map<Long, Boolean> ingredientExpiring = new HashMap<>();

        for (PantryItem item : pantryItems) {
            boolean expiring = isExpiringSoon(item.getExpireDate(), today);
            if (item.getIngredientId() != null) {
                registerPantryIngredient(item.getIngredientId(), expiring, ingredientIds, ingredientNames, ingredientExpiring, namesByIngredientId);
            }
            resolveIngredientId(item.getRawName(), ingredientIdsByName, ingredientIdsByAlias)
                    .ifPresent(id -> registerPantryIngredient(id, expiring, ingredientIds, ingredientNames, ingredientExpiring, namesByIngredientId));
            IngredientAliasCatalog.expandSynonyms(IngredientNameNormalizer.normalize(item.getRawName()), ingredientNames);
        }

        return new PantryContext(ingredientIds, ingredientNames, ingredientExpiring);
    }

    private void registerPantryIngredient(Long ingredientId,
                                          boolean expiring,
                                          Set<Long> ingredientIds,
                                          Set<String> ingredientNames,
                                          Map<Long, Boolean> ingredientExpiring,
                                          Map<Long, Set<String>> namesByIngredientId) {
        ingredientIds.add(ingredientId);
        ingredientExpiring.merge(ingredientId, expiring, (left, right) -> left || right);
        namesByIngredientId.getOrDefault(ingredientId, Set.of()).forEach(ingredientNames::add);
    }

    private Map<Long, Set<String>> buildNamesByIngredientId(Map<String, Long> ingredientIdsByName,
                                                              List<IngredientAlias> aliases) {
        Map<Long, Set<String>> namesByIngredientId = new HashMap<>();
        ingredientIdsByName.forEach((name, id) ->
                namesByIngredientId.computeIfAbsent(id, ignored -> new HashSet<>()).add(name));
        for (IngredientAlias alias : aliases) {
            namesByIngredientId.computeIfAbsent(alias.getIngredientId(), ignored -> new HashSet<>())
                    .add(alias.getNormalizedAlias());
        }
        return namesByIngredientId;
    }

    private RecommendationDtos.RecipeMatch match(Recipe recipe,
                                                   List<RecipeIngredient> allIngredients,
                                                   PantryContext pantry,
                                                   Map<String, Long> ingredientIdsByName,
                                                   Map<String, Long> ingredientIdsByAlias,
                                                   String coverImageUrl,
                                                   int ingredientCount) {
        Map<String, RecipeIngredient> uniqueRequired = new LinkedHashMap<>();
        allIngredients.stream()
                .filter(item -> item.getSourceSection() == RecipeEnums.SourceSection.REQUIRED)
                .filter(item -> !item.isOptional() && item.getRole() != RecipeEnums.IngredientRole.TOOL)
                .forEach(item -> uniqueRequired.putIfAbsent(ingredientKey(item, ingredientIdsByName, ingredientIdsByAlias), item));
        List<RecipeIngredient> required = new ArrayList<>(uniqueRequired.values());
        if (required.isEmpty()) {
            return null;
        }

        List<String> matched = new ArrayList<>();
        List<String> missing = new ArrayList<>();
        List<String> expiringMatched = new ArrayList<>();
        for (RecipeIngredient item : required) {
            if (isIngredientAvailable(item, pantry, ingredientIdsByName, ingredientIdsByAlias)) {
                matched.add(item.getRawName());
                if (usesExpiringIngredient(item, pantry, ingredientIdsByName, ingredientIdsByAlias)) {
                    expiringMatched.add(item.getRawName());
                }
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
        int missingCount = missing.size();
        return new RecommendationDtos.RecipeMatch(
                recipe.getId(), recipe.getName(), recipe.getCategory(), recipe.getDescription(),
                recipe.getDifficultyText(), recipe.getDifficultyLevel(), recipe.getCalories(),
                coverImageUrl, ingredientCount, matchedCount, requiredCount,
                missingCount, rate,
                matched.stream().distinct().toList(),
                missing.stream().distinct().toList(),
                expiringMatched.stream().distinct().toList(),
                expiringMatched.size(),
                missingCount <= READY_NOW_MAX_MISSING);
    }

    private boolean isIngredientAvailable(RecipeIngredient item,
                                          PantryContext pantry,
                                          Map<String, Long> ingredientIdsByName,
                                          Map<String, Long> ingredientIdsByAlias) {
        if (item.getIngredientId() != null && pantry.ingredientIds.contains(item.getIngredientId())) {
            return true;
        }
        String normalizedRawName = IngredientNameNormalizer.normalize(item.getRawName());
        if (pantry.ingredientNames.contains(normalizedRawName)) {
            return true;
        }
        Set<String> recipeNames = new HashSet<>();
        IngredientAliasCatalog.expandSynonyms(normalizedRawName, recipeNames);
        for (String name : recipeNames) {
            if (pantry.ingredientNames.contains(name)) {
                return true;
            }
        }
        return resolveIngredientId(item.getRawName(), ingredientIdsByName, ingredientIdsByAlias)
                .map(pantry.ingredientIds::contains)
                .orElse(false);
    }

    private boolean usesExpiringIngredient(RecipeIngredient item,
                                           PantryContext pantry,
                                           Map<String, Long> ingredientIdsByName,
                                           Map<String, Long> ingredientIdsByAlias) {
        if (item.getIngredientId() != null && Boolean.TRUE.equals(pantry.ingredientExpiring.get(item.getIngredientId()))) {
            return true;
        }
        return resolveIngredientId(item.getRawName(), ingredientIdsByName, ingredientIdsByAlias)
                .map(id -> Boolean.TRUE.equals(pantry.ingredientExpiring.get(id)))
                .orElse(false);
    }

    private Map<Long, String> loadCoverImageUrls(Collection<Long> recipeIds) {
        if (recipeIds.isEmpty()) return Map.of();
        Map<Long, String> coverImageUrls = new HashMap<>();
        for (var media : mediaRepository.findByRecipeIdInOrderByRecipeIdAscSortOrderAsc(recipeIds)) {
            coverImageUrls.putIfAbsent(media.getRecipeId(),
                    "/api/v1/recipes/" + media.getRecipeId() + "/media/" + media.getSortOrder());
        }
        return coverImageUrls;
    }

    private String ingredientKey(RecipeIngredient item,
                                 Map<String, Long> ingredientIdsByName,
                                 Map<String, Long> ingredientIdsByAlias) {
        if (item.getIngredientId() != null) {
            return "id:" + item.getIngredientId();
        }
        return resolveIngredientId(item.getRawName(), ingredientIdsByName, ingredientIdsByAlias)
                .map(id -> "id:" + id)
                .orElseGet(() -> "raw:" + IngredientNameNormalizer.normalize(item.getRawName()));
    }

    private Optional<Long> resolveIngredientId(String rawName,
                                               Map<String, Long> ingredientIdsByName,
                                               Map<String, Long> ingredientIdsByAlias) {
        String normalized = IngredientNameNormalizer.normalize(rawName);
        Optional<Long> resolved = Optional.ofNullable(ingredientIdsByName.get(normalized))
                .or(() -> Optional.ofNullable(ingredientIdsByAlias.get(normalized)));
        if (resolved.isPresent()) {
            return resolved;
        }
        String canonical = IngredientAliasCatalog.COMMON.get(normalized);
        if (canonical == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(ingredientIdsByName.get(canonical))
                .or(() -> Optional.ofNullable(ingredientIdsByAlias.get(canonical)));
    }

    private boolean isExpiringSoon(LocalDate expireDate, LocalDate today) {
        return expireDate != null
                && !expireDate.isBefore(today)
                && !expireDate.isAfter(today.plusDays(EXPIRING_WITHIN_DAYS));
    }

    private record PantryContext(
            Set<Long> ingredientIds,
            Set<String> ingredientNames,
            Map<Long, Boolean> ingredientExpiring
    ) {
    }
}
