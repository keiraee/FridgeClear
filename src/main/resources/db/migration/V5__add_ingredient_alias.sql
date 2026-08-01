CREATE TABLE IF NOT EXISTS ingredient_alias (
    id BIGINT NOT NULL AUTO_INCREMENT,
    ingredient_id BIGINT NOT NULL,
    alias_name VARCHAR(128) NOT NULL,
    normalized_alias VARCHAR(128) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_ingredient_alias_normalized (normalized_alias),
    CONSTRAINT fk_ingredient_alias_ingredient FOREIGN KEY (ingredient_id) REFERENCES ingredient (id),
    INDEX idx_ingredient_alias_ingredient (ingredient_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
