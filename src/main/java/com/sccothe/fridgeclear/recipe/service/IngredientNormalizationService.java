package com.sccothe.fridgeclear.recipe.service;

import com.sccothe.fridgeclear.recipe.domain.Ingredient;
import com.sccothe.fridgeclear.recipe.domain.IngredientAlias;
import com.sccothe.fridgeclear.recipe.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
public class IngredientNormalizationService {
    private static final Map<String, String> COMMON_ALIASES = Map.of(
            "西红柿", "番茄",
            "蕃茄", "番茄",
            "马铃薯", "土豆",
            "洋芋", "土豆",
            "小葱", "葱",
            "大葱", "葱",
            "酱油", "生抽"
    );

    private final IngredientRepository ingredientRepository;
    private final IngredientAliasRepository aliasRepository;
    private final RecipeIngredientRepository recipeIngredientRepository;

    public IngredientNormalizationService(IngredientRepository ingredientRepository,
                                           IngredientAliasRepository aliasRepository,
                                           RecipeIngredientRepository recipeIngredientRepository) {
        this.ingredientRepository = ingredientRepository;
        this.aliasRepository = aliasRepository;
        this.recipeIngredientRepository = recipeIngredientRepository;
    }

    @Transactional
    public NormalizeReport normalizeAll() {
        int scanned = 0, changed = 0, merged = 0, aliases = 0;
        List<String> messages = new ArrayList<>();
        for (Ingredient ingredient : ingredientRepository.findAll()) {
            scanned++;
            String originalName = ingredient.getCanonicalName();
            String original = IngredientNameNormalizer.normalize(originalName);
            String targetName = COMMON_ALIASES.getOrDefault(original, original);
            if (targetName.equals(original)) continue;

            Ingredient target = ingredientRepository.findByNormalizedName(targetName).orElse(null);
            if (target == null || Objects.equals(target.getId(), ingredient.getId())) {
                ingredient.setCanonicalName(targetName);
                ingredient.setNormalizedName(targetName);
                ingredientRepository.save(ingredient);
                target = ingredient;
                changed++;
            } else {
                Long sourceId = ingredient.getId();
                Long targetId = target.getId();
                recipeIngredientRepository.findAll().stream()
                        .filter(item -> Objects.equals(item.getIngredientId(), sourceId))
                        .forEach(item -> {
                            item.setIngredientId(targetId);
                            recipeIngredientRepository.save(item);
                        });
                ingredientRepository.delete(ingredient);
                merged++;
            }
            if (!aliasRepository.existsByNormalizedAlias(original)) {
                IngredientAlias alias = new IngredientAlias();
                alias.setIngredientId(target.getId());
                alias.setAliasName(originalName);
                alias.setNormalizedAlias(original);
                aliasRepository.save(alias);
                aliases++;
            }
            messages.add(original + " -> " + targetName);
        }
        return new NormalizeReport(scanned, changed, merged, aliases, messages);
    }

    public record NormalizeReport(int scanned, int changed, int merged, int aliases, List<String> messages) {}
}
