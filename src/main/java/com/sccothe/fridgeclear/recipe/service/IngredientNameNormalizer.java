package com.sccothe.fridgeclear.recipe.service;

import java.util.Locale;

final class IngredientNameNormalizer {
    private IngredientNameNormalizer() {
    }

    static String normalize(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT).replaceAll("\\s+", "").trim();
    }
}
