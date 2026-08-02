package com.sccothe.fridgeclear.recipe.service;

import java.util.Locale;

public final class IngredientNameNormalizer {
    private IngredientNameNormalizer() {
    }

    public static String normalize(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT).replaceAll("\\s+", "").trim();
    }
}
