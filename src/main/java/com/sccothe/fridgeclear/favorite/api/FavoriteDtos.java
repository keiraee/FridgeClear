package com.sccothe.fridgeclear.favorite.api;

import java.util.List;

public final class FavoriteDtos {
    private FavoriteDtos() {}

    public record IdListResponse(List<Long> recipeIds) {}
}
