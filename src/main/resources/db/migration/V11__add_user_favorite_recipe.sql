CREATE TABLE IF NOT EXISTS user_favorite_recipe (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    recipe_id BIGINT NOT NULL,
    created_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_favorite_recipe_user_recipe (user_id, recipe_id),
    INDEX idx_user_favorite_recipe_user_id (user_id),
    INDEX idx_user_favorite_recipe_recipe_id (recipe_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
