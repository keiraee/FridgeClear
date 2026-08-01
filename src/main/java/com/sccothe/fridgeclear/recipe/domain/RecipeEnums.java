package com.sccothe.fridgeclear.recipe.domain;

public final class RecipeEnums {
    private RecipeEnums() {}

    public enum Category { AQUATIC, BREAKFAST, CONDIMENT, DESSERT, DRINK, MEAT_DISH, SEMI_FINISHED, SOUP, STAPLE, VEGETABLE_DISH, UNKNOWN }
    public enum Status { ACTIVE, HIDDEN }
    public enum ImportStatus { SUCCESS, PARTIAL, FAILED }
    public enum IngredientType { FOOD, SEASONING, TOOL, UNKNOWN }
    public enum IngredientRole { MAIN, SEASONING, TOOL, UNKNOWN }
    public enum QuantityParseStatus { PARSED, PARTIAL, UNPARSED }
    public enum SourceSection { REQUIRED, CALCULATION, OPERATION }
    public enum MediaType { IMAGE }
}
