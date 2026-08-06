package com.sccothe.fridgeclear.recipe.service;

import java.util.Map;

/** 运行时同义词表，与 {@link IngredientNormalizationService} 入库归一化保持一致。 */
public final class IngredientAliasCatalog {
    public static final Map<String, String> COMMON = Map.ofEntries(
            Map.entry("西红柿", "番茄"), Map.entry("蕃茄", "番茄"),
            Map.entry("马铃薯", "土豆"), Map.entry("洋芋", "土豆"),
            Map.entry("小葱", "葱"), Map.entry("大葱", "葱"), Map.entry("香葱", "葱"),
            Map.entry("大蒜", "蒜"), Map.entry("蒜瓣", "蒜"),
            Map.entry("生姜", "姜"), Map.entry("食盐", "盐"), Map.entry("酱油", "生抽")
    );

    private IngredientAliasCatalog() {
    }

    public static void expandSynonyms(String normalizedName, java.util.Set<String> target) {
        if (normalizedName == null || normalizedName.isBlank()) {
            return;
        }
        target.add(normalizedName);
        String canonical = COMMON.get(normalizedName);
        if (canonical != null) {
            target.add(canonical);
        }
        COMMON.forEach((alias, canon) -> {
            if (canon.equals(normalizedName)) {
                target.add(alias);
            }
        });
    }
}
