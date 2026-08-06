package com.sccothe.fridgeclear.recipe.api;

import java.util.Locale;

public enum RecommendationFilter {
    ALL,
    HIGH_MATCH,
    READY_NOW;

    public static RecommendationFilter fromParam(String value) {
        if (value == null || value.isBlank()) {
            return ALL;
        }
        return switch (value.toLowerCase(Locale.ROOT)) {
            case "high_match", "high-match", "highmatch" -> HIGH_MATCH;
            case "ready_now", "ready-now", "readynow",
                 "cook_tonight", "cook-tonight", "cooktonight" -> READY_NOW;
            default -> ALL;
        };
    }
}
