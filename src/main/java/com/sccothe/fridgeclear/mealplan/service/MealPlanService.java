package com.sccothe.fridgeclear.mealplan.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.sccothe.fridgeclear.ai.service.AiChatGateway;
import com.sccothe.fridgeclear.auth.service.CurrentUser;
import com.sccothe.fridgeclear.mealplan.api.MealPlanDtos;
import com.sccothe.fridgeclear.mealplan.domain.*;
import com.sccothe.fridgeclear.mealplan.repository.*;
import com.sccothe.fridgeclear.common.api.ResourceNotFoundException;
import com.sccothe.fridgeclear.pantry.domain.PantryItem;
import com.sccothe.fridgeclear.pantry.domain.PantryItemStatus;
import com.sccothe.fridgeclear.pantry.repository.PantryItemRepository;
import com.sccothe.fridgeclear.recipe.domain.Recipe;
import com.sccothe.fridgeclear.recipe.domain.RecipeEnums;
import com.sccothe.fridgeclear.recipe.domain.RecipeIngredient;
import com.sccothe.fridgeclear.recipe.repository.RecipeRepository;
import com.sccothe.fridgeclear.recipe.repository.RecipeIngredientQueryRepository;
import com.sccothe.fridgeclear.recipe.repository.IngredientRepository;
import com.sccothe.fridgeclear.recipe.repository.RecipeMediaQueryRepository;
import com.sccothe.fridgeclear.recipe.service.IngredientNameNormalizer;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class MealPlanService {
    private static final String PROMPT_VERSION = "meal-plan-v1";
    private final PantryItemRepository pantryRepository;
    private final RecipeRepository recipeRepository;
    private final RecipeIngredientQueryRepository recipeIngredientRepository;
    private final IngredientRepository ingredientRepository;
    private final RecipeMediaQueryRepository mediaRepository;
    private final AiChatGateway aiGateway;
    private final AiPlanRunRepository aiRunRepository;
    private final MealPlanRepository mealPlanRepository;
    private final MealPlanItemRepository itemRepository;
    private final ShoppingListItemRepository shoppingRepository;
    private final ObjectMapper objectMapper;

    public MealPlanService(PantryItemRepository pantryRepository, RecipeRepository recipeRepository,
                           RecipeIngredientQueryRepository recipeIngredientRepository,
                           IngredientRepository ingredientRepository,
                           RecipeMediaQueryRepository mediaRepository,
                           AiChatGateway aiGateway, AiPlanRunRepository aiRunRepository,
                           MealPlanRepository mealPlanRepository, MealPlanItemRepository itemRepository,
                           ShoppingListItemRepository shoppingRepository, ObjectMapper objectMapper) {
        this.pantryRepository = pantryRepository;
        this.recipeRepository = recipeRepository;
        this.recipeIngredientRepository = recipeIngredientRepository;
        this.ingredientRepository = ingredientRepository;
        this.mediaRepository = mediaRepository;
        this.aiGateway = aiGateway;
        this.aiRunRepository = aiRunRepository;
        this.mealPlanRepository = mealPlanRepository;
        this.itemRepository = itemRepository;
        this.shoppingRepository = shoppingRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public MealPlanDtos.Response generate(MealPlanDtos.GenerateRequest request) {
        Long userId = CurrentUser.id();
        List<PantryItem> pantry = pantryRepository.findByUserIdAndStatusOrderByExpireDateAscIdAsc(
                userId, PantryItemStatus.AVAILABLE);
        if (request.usePantryItemIds() != null && !request.usePantryItemIds().isEmpty()) {
            Set<Long> selected = new HashSet<>(request.usePantryItemIds());
            pantry = pantry.stream().filter(item -> selected.contains(item.getId())).toList();
        }
        List<Recipe> recipes = recipeRepository.findByStatus(RecipeEnums.Status.ACTIVE, PageRequest.of(0, 80)).getContent();
        if (recipes.isEmpty()) throw new IllegalStateException("暂无可用菜谱，请先导入 HowToCook 数据");
        Map<Long, List<RecipeIngredient>> ingredientsByRecipe = recipeIngredientRepository.findByRecipeIdIn(
                        recipes.stream().map(Recipe::getId).toList()).stream()
                .collect(Collectors.groupingBy(RecipeIngredient::getRecipeId));

        String requestJson = writeJson(request);
        AiPlanRun run = new AiPlanRun();
        run.setUserId(userId);
        run.setPromptVersion(PROMPT_VERSION);
        run.setRequestJson(requestJson);
        run.setStatus(MealPlanEnums.AiRunStatus.RUNNING);
        run.setModelName("unknown");
        run = aiRunRepository.save(run);

        try {
            AiChatGateway.ChatResult result = aiGateway.complete(systemPrompt(), userPrompt(request, pantry, recipes, ingredientsByRecipe));
            run.setModelName(result.modelName());
            run.setResponseJson(result.content());
            run.setStatus(MealPlanEnums.AiRunStatus.SUCCESS);
            run.setFinishedAt(java.time.LocalDateTime.now());
            aiRunRepository.save(run);
            archiveActivePlans(userId);
            return persistPlan(request, pantry, recipes, ingredientsByRecipe, run, result.content(), userId);
        } catch (RuntimeException exception) {
            run.setStatus(MealPlanEnums.AiRunStatus.FAILED);
            run.setErrorMessage(exception.getMessage());
            run.setFinishedAt(java.time.LocalDateTime.now());
            aiRunRepository.save(run);
            throw exception;
        }
    }

    @Transactional(readOnly = true)
    public MealPlanDtos.PageResponse list(int page, int size, MealPlanEnums.PlanStatus status) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 100);
        var pageable = PageRequest.of(safePage, safeSize, org.springframework.data.domain.Sort.by("startDate").descending());
        Long userId = CurrentUser.id();
        var result = status == null ? mealPlanRepository.findByUserId(userId, pageable)
                : mealPlanRepository.findByUserIdAndStatus(userId, status, pageable);
        return new MealPlanDtos.PageResponse(result.getContent().stream()
                .map(item -> new MealPlanDtos.ListItem(item.getId(), item.getTitle(), item.getStartDate(), item.getEndDate(), item.getStatus())).toList(),
                result.getNumber(), result.getSize(), result.getTotalElements(), result.getTotalPages());
    }

    @Transactional
    public void archive(Long id) {
        MealPlan plan = ownedPlan(id);
        plan.setStatus(MealPlanEnums.PlanStatus.ARCHIVED);
        mealPlanRepository.save(plan);
    }

    private void archiveActivePlans(Long userId) {
        List<MealPlan> activePlans = mealPlanRepository.findByUserIdAndStatus(userId, MealPlanEnums.PlanStatus.ACTIVE);
        activePlans.forEach(plan -> plan.setStatus(MealPlanEnums.PlanStatus.ARCHIVED));
        mealPlanRepository.saveAll(activePlans);
    }

    @Transactional(readOnly = true)
    public MealPlanDtos.DetailResponse detail(Long id) {
        MealPlan plan = ownedPlan(id);
        List<MealPlanItem> items = itemRepository.findByMealPlanIdOrderByPlanDateAscSortOrderAsc(id);
        List<Long> recipeIds = items.stream().map(MealPlanItem::getRecipeId).distinct().toList();
        Map<Long, String> recipeNames = recipeRepository.findAllById(recipeIds).stream()
                .collect(Collectors.toMap(Recipe::getId, Recipe::getName, (first, ignored) -> first));
        Map<Long, String> coverImageUrls = loadCoverImageUrls(recipeIds);
        List<MealPlanDtos.ItemResponse> itemResponses = items.stream()
                .map(item -> toItemResponse(item, recipeNames.getOrDefault(item.getRecipeId(), "未知菜谱"),
                        coverImageUrls.get(item.getRecipeId())))
                .toList();
        return new MealPlanDtos.DetailResponse(plan.getId(), plan.getTitle(), plan.getStartDate(), plan.getEndDate(), plan.getStatus(),
                itemResponses, shoppingResponses(id));
    }

    @Transactional
    public MealPlanDtos.ItemResponse updateItemStatus(Long planId, Long itemId, MealPlanDtos.ItemStatusRequest request) {
        ownedPlan(planId);
        MealPlanItem item = itemRepository.findByIdAndMealPlanId(itemId, planId)
                .orElseThrow(() -> new ResourceNotFoundException("备餐计划项不存在: " + itemId));
        item.setStatus(request.status());
        itemRepository.save(item);
        String recipeName = recipeRepository.findById(item.getRecipeId()).map(Recipe::getName).orElse("未知菜谱");
        String coverImageUrl = loadCoverImageUrls(List.of(item.getRecipeId())).get(item.getRecipeId());
        return toItemResponse(item, recipeName, coverImageUrl);
    }

    @Transactional(readOnly = true)
    public List<MealPlanDtos.ShoppingResponse> shoppingList(Long planId) {
        ownedPlan(planId);
        return shoppingResponses(planId);
    }

    @Transactional
    public MealPlanDtos.ShoppingResponse updateShoppingStatus(Long itemId, MealPlanDtos.ShoppingStatusRequest request) {
        ShoppingListItem item = shoppingRepository.findById(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("采购清单项不存在: " + itemId));
        ownedPlan(item.getMealPlanId());
        item.setStatus(request.status());
        item = shoppingRepository.save(item);
        return toShoppingResponse(item);
    }

    private MealPlan ownedPlan(Long id) {
        return mealPlanRepository.findByIdAndUserId(id, CurrentUser.id())
                .orElseThrow(() -> new ResourceNotFoundException("备餐计划不存在: " + id));
    }

    private List<MealPlanDtos.ShoppingResponse> shoppingResponses(Long planId) {
        return shoppingRepository.findByMealPlanIdOrderByIdAsc(planId).stream().map(this::toShoppingResponse).toList();
    }

    private MealPlanDtos.ItemResponse toItemResponse(MealPlanItem item, String recipeName, String coverImageUrl) {
        return new MealPlanDtos.ItemResponse(item.getId(), item.getPlanDate(), item.getMealType(),
                new MealPlanDtos.RecipeResponse(item.getRecipeId(), recipeName, coverImageUrl), item.getServings(),
                readStringList(item.getUsedIngredientsJson()), readStringList(item.getMissingIngredientsJson()),
                item.getReason(), item.getStatus());
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

    private MealPlanDtos.ShoppingResponse toShoppingResponse(ShoppingListItem item) {
        return new MealPlanDtos.ShoppingResponse(item.getId(), item.getName(), item.getQuantity(), item.getUnit(), item.getReason(), item.getStatus());
    }

    private List<String> readStringList(String json) {
        if (json == null || json.isBlank()) return List.of();
        try { return objectMapper.readValue(json, new TypeReference<>() {}); }
        catch (Exception ignored) { return List.of(); }
    }

    private MealPlanDtos.Response persistPlan(MealPlanDtos.GenerateRequest request, List<PantryItem> pantry,
                                              List<Recipe> recipes, Map<Long, List<RecipeIngredient>> ingredientsByRecipe,
                                              AiPlanRun run, String content, Long userId) {
        JsonNode root = parseJson(content);
        Map<Long, Recipe> recipeMap = recipes.stream().collect(Collectors.toMap(Recipe::getId, Function.identity()));
        LocalDate start = LocalDate.now();
        MealPlan plan = new MealPlan();
        plan.setUserId(userId);
        plan.setAiPlanRunId(run.getId());
        plan.setTitle("AI 冰箱消耗计划");
        plan.setStartDate(start);
        plan.setEndDate(start.plusDays(request.days() - 1L));
        plan.setStatus(MealPlanEnums.PlanStatus.ACTIVE);
        plan.setConstraintsJson(writeJson(request));
        plan = mealPlanRepository.save(plan);

        List<MealPlanDtos.ItemResponse> itemResponses = new ArrayList<>();
        JsonNode items = root.path("items");
        int sort = 0;
        if (items.isArray()) {
            for (JsonNode item : items) {
                Long recipeId = longValue(item, "recipeId");
                Recipe recipe = recipeMap.get(recipeId);
                if (recipe == null) continue;
                MealPlanItem entity = new MealPlanItem();
                entity.setMealPlanId(plan.getId());
                // 计划日期由后端根据天数和餐次数量生成，避免 AI 返回过期或错误年份的日期。
                entity.setPlanDate(start.plusDays(sort / Math.max(request.mealTypes().size(), 1)));
                entity.setMealType(enumValue(item, "mealType", MealPlanEnums.MealType.class, request.mealTypes().get(0)));
                entity.setRecipeId(recipeId);
                // 用用户请求的人数作为最终份数，避免 AI 返回的 servings 偏离用户输入。
                entity.setServings(BigDecimal.valueOf(request.peopleCount()));
                IngredientMatch match = matchIngredients(pantry, ingredientsByRecipe.getOrDefault(recipeId, List.of()));
                entity.setUsedIngredientsJson(writeJson(match.usedNames()));
                entity.setMissingIngredientsJson(writeJson(match.missingNames()));
                entity.setReason(textValue(item, "reason", "根据库存和过期时间生成"));
                entity.setStatus(MealPlanEnums.ItemStatus.PLANNED);
                entity.setSortOrder(sort++);
                entity = itemRepository.save(entity);
                itemResponses.add(new MealPlanDtos.ItemResponse(entity.getId(), entity.getPlanDate(), entity.getMealType(),
                        new MealPlanDtos.RecipeResponse(recipe.getId(), recipe.getName(), null), entity.getServings(),
                        match.usedNames(), match.missingNames(), entity.getReason(), entity.getStatus()));
            }
        }
        if (itemResponses.isEmpty()) throw new IllegalStateException("AI 返回的菜谱无法匹配本地菜谱");

        Map<Long, String> coverImageUrls = loadCoverImageUrls(
                itemResponses.stream().map(item -> item.recipe().id()).distinct().toList());
        itemResponses = itemResponses.stream()
                .map(item -> new MealPlanDtos.ItemResponse(item.id(), item.planDate(), item.mealType(),
                        new MealPlanDtos.RecipeResponse(item.recipe().id(), item.recipe().name(),
                                coverImageUrls.get(item.recipe().id())),
                        item.servings(), item.usedIngredients(), item.missingIngredients(), item.reason(), item.status()))
                .toList();

        Map<Long, String> canonicalNames = ingredientRepository.findAll().stream()
                .collect(Collectors.toMap(com.sccothe.fridgeclear.recipe.domain.Ingredient::getId,
                        com.sccothe.fridgeclear.recipe.domain.Ingredient::getCanonicalName, (first, ignored) -> first));
        List<MealPlanDtos.ShoppingResponse> shoppingResponses = new ArrayList<>();
        Map<String, MissingIngredient> shopping = new LinkedHashMap<>();
        for (MealPlanDtos.ItemResponse item : itemResponses) {
            Recipe recipe = recipeMap.get(item.recipe().id());
            for (RecipeIngredient ingredient : ingredientsByRecipe.getOrDefault(recipe.getId(), List.of())) {
                if (ingredient.getSourceSection() != RecipeEnums.SourceSection.REQUIRED || ingredient.isOptional()
                        || ingredient.getRole() == RecipeEnums.IngredientRole.TOOL) continue;
                IngredientMatch match = matchIngredients(pantry, ingredientsByRecipe.getOrDefault(recipe.getId(), List.of()));
                if (!match.missingKeys().contains(ingredientKey(ingredient))) continue;
                String key = ingredient.getIngredientId() == null
                        ? IngredientNameNormalizer.normalize(ingredient.getRawName())
                        : "id:" + ingredient.getIngredientId();
                String displayName = ingredient.getIngredientId() == null
                        ? ingredient.getRawName() : canonicalNames.getOrDefault(ingredient.getIngredientId(), ingredient.getRawName());
                MissingIngredient current = shopping.get(key);
                BigDecimal quantity = ingredient.getQuantityMin();
                shopping.put(key, current == null
                        ? new MissingIngredient(displayName, quantity, ingredient.getUnit(), "计划中需要")
                        : current.add(quantity));
            }
        }
        for (MissingIngredient entry : shopping.values()) {
            ShoppingListItem entity = new ShoppingListItem();
            entity.setMealPlanId(plan.getId());
            entity.setName(entry.name());
            entity.setQuantity(entry.quantity());
            entity.setUnit(entry.unit());
            entity.setReason(entry.reason());
            entity.setStatus(MealPlanEnums.ShoppingStatus.TODO);
            entity = shoppingRepository.save(entity);
            shoppingResponses.add(new MealPlanDtos.ShoppingResponse(entity.getId(), entity.getName(), entity.getQuantity(), entity.getUnit(), entity.getReason(), entity.getStatus()));
        }
        String summary = textValue(root, "summary", "已根据库存生成备餐计划");
        Map<String, PantryItem> earliestByIngredient = new LinkedHashMap<>();
        pantry.stream().filter(item -> item.getExpireDate() != null).forEach(item -> {
            String key = item.getIngredientId() == null ? IngredientNameNormalizer.normalize(item.getRawName()) : "id:" + item.getIngredientId();
            earliestByIngredient.putIfAbsent(key, item);
        });
        List<MealPlanDtos.ExpiringIngredient> expiring = earliestByIngredient.values().stream().limit(10)
                .map(item -> new MealPlanDtos.ExpiringIngredient(item.getId(), item.getRawName(), item.getExpireDate(), "优先消耗临近过期食材")).toList();
        return new MealPlanDtos.Response(plan.getId(), summary, expiring, itemResponses, shoppingResponses);
    }

    private String systemPrompt() {
        return "你是 FridgeClear 的备餐规划 AI。只能从候选菜谱中选择 recipeId，必须返回 JSON，不要 Markdown。JSON 格式：" +
                "{summary:string,items:[{planDate:yyyy-MM-dd,mealType:BREAKFAST|LUNCH|DINNER|SNACK,recipeId:number,servings:number,usedIngredients:string[],missingIngredients:string[],reason:string}],shoppingList:[{name:string,quantity:number,unit:string,reason:string}]}。" +
                "优先使用临近过期库存，遵守用户忌口、设备和烹饪时长。";
    }

    private String userPrompt(MealPlanDtos.GenerateRequest request, List<PantryItem> pantry, List<Recipe> recipes,
                              Map<Long, List<RecipeIngredient>> ingredientsByRecipe) {
        Map<String, Object> context = new LinkedHashMap<>();
        context.put("constraints", request);
        context.put("pantry", pantry.stream().map(item -> Map.of("id", item.getId(), "name", item.getRawName(), "quantity", item.getQuantity() == null ? "" : item.getQuantity(), "unit", item.getUnit() == null ? "" : item.getUnit(), "expireDate", item.getExpireDate() == null ? "" : item.getExpireDate())).toList());
        context.put("candidateRecipes", recipes.stream().map(recipe -> Map.of(
                "recipeId", recipe.getId(), "name", recipe.getName(), "category", recipe.getCategory().name(),
                "difficulty", recipe.getDifficultyLevel() == null ? "" : recipe.getDifficultyLevel(),
                "requiredIngredients", ingredientsByRecipe.getOrDefault(recipe.getId(), List.of()).stream()
                        .filter(item -> item.getSourceSection() == RecipeEnums.SourceSection.REQUIRED)
                        .filter(item -> !item.isOptional() && item.getRole() != RecipeEnums.IngredientRole.TOOL)
                        .map(item -> item.getRawName() + (item.getRawQuantity() == null ? "" : "(" + item.getRawQuantity() + ")"))
                        .distinct().toList()
        )).toList());
        return writeJson(context);
    }

    private IngredientMatch matchIngredients(List<PantryItem> pantry, List<RecipeIngredient> ingredients) {
        Set<Long> pantryIds = pantry.stream().map(PantryItem::getIngredientId).filter(Objects::nonNull).collect(Collectors.toSet());
        Set<String> pantryNames = pantry.stream().map(item -> IngredientNameNormalizer.normalize(item.getRawName())).collect(Collectors.toSet());
        Map<String, RecipeIngredient> unique = new LinkedHashMap<>();
        ingredients.stream().filter(item -> item.getSourceSection() == RecipeEnums.SourceSection.REQUIRED)
                .filter(item -> !item.isOptional() && item.getRole() != RecipeEnums.IngredientRole.TOOL)
                .forEach(item -> unique.putIfAbsent(ingredientKey(item), item));
        List<String> used = new ArrayList<>();
        List<String> missing = new ArrayList<>();
        Set<String> missingKeys = new HashSet<>();
        for (RecipeIngredient item : unique.values()) {
            boolean available = (item.getIngredientId() != null && pantryIds.contains(item.getIngredientId()))
                    || pantryNames.contains(IngredientNameNormalizer.normalize(item.getRawName()));
            if (available) used.add(item.getRawName());
            else { missing.add(item.getRawName()); missingKeys.add(ingredientKey(item)); }
        }
        return new IngredientMatch(used, missing, missingKeys);
    }

    private String ingredientKey(RecipeIngredient item) {
        return item.getIngredientId() == null ? "raw:" + IngredientNameNormalizer.normalize(item.getRawName()) : "id:" + item.getIngredientId();
    }

    private record IngredientMatch(List<String> usedNames, List<String> missingNames, Set<String> missingKeys) {}
    private record MissingIngredient(String name, BigDecimal quantity, String unit, String reason) {
        MissingIngredient add(BigDecimal value) {
            return new MissingIngredient(name, quantity == null || value == null ? quantity : quantity.add(value), unit, reason);
        }
    }

    private JsonNode parseJson(String content) {
        String clean = content.trim().replaceFirst("^```(?:json)?\\s*", "").replaceFirst("\\s*```$", "");
        try { return objectMapper.readTree(clean); } catch (Exception exception) { throw new IllegalStateException("AI 返回内容不是有效 JSON", exception); }
    }
    private String writeJson(Object value) { try { return objectMapper.writeValueAsString(value); } catch (Exception e) { throw new IllegalStateException("JSON 序列化失败", e); } }
    private List<String> stringList(JsonNode node) { if (!node.isArray()) return List.of(); List<String> result = new ArrayList<>(); node.forEach(item -> result.add(item.asText())); return result; }
    private String textValue(JsonNode node, String field, String fallback) { return node.hasNonNull(field) && !node.get(field).asText().isBlank() ? node.get(field).asText() : fallback; }
    private Long longValue(JsonNode node, String field) { return node.hasNonNull(field) && node.get(field).canConvertToLong() ? node.get(field).asLong() : null; }
    private BigDecimal decimalValue(JsonNode node, String field, BigDecimal fallback) { try { return node.hasNonNull(field) ? new BigDecimal(node.get(field).asText()) : fallback; } catch (Exception e) { return fallback; } }
    private LocalDate dateValue(JsonNode node, String field, LocalDate fallback) { try { return LocalDate.parse(textValue(node, field, fallback.toString())); } catch (Exception e) { return fallback; } }
    private <T extends Enum<T>> T enumValue(JsonNode node, String field, Class<T> type, T fallback) { try { return Enum.valueOf(type, textValue(node, field, fallback.name())); } catch (Exception e) { return fallback; } }
}
