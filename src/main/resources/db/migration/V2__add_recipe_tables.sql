CREATE TABLE IF NOT EXISTS recipe_source_document (
    id BIGINT NOT NULL AUTO_INCREMENT,
    source_repository VARCHAR(255) NOT NULL,
    source_commit VARCHAR(64) NOT NULL,
    source_path VARCHAR(512) NOT NULL,
    source_identity_hash CHAR(64) NOT NULL,
    file_hash VARCHAR(64) NOT NULL,
    raw_markdown LONGTEXT NOT NULL,
    parser_version VARCHAR(32) NOT NULL,
    import_status VARCHAR(32) NOT NULL,
    import_error TEXT NULL,
    imported_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_source_document_identity (source_identity_hash),
    INDEX idx_source_document_path (source_path(191))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS recipe (
    id BIGINT NOT NULL AUTO_INCREMENT,
    source_document_id BIGINT NOT NULL,
    name VARCHAR(128) NOT NULL,
    slug VARCHAR(160) NOT NULL,
    category VARCHAR(32) NOT NULL,
    description TEXT NULL,
    difficulty_text VARCHAR(32) NULL,
    difficulty_level TINYINT NULL,
    calories DECIMAL(10,2) NULL,
    source_title VARCHAR(160) NULL,
    status VARCHAR(32) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_recipe_source_document (source_document_id),
    UNIQUE KEY uk_recipe_slug (slug),
    CONSTRAINT fk_recipe_source_document FOREIGN KEY (source_document_id) REFERENCES recipe_source_document (id),
    INDEX idx_recipe_category_status (category, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS ingredient (
    id BIGINT NOT NULL AUTO_INCREMENT,
    canonical_name VARCHAR(128) NOT NULL,
    normalized_name VARCHAR(128) NOT NULL,
    ingredient_type VARCHAR(32) NOT NULL,
    default_unit VARCHAR(32) NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_ingredient_normalized_name (normalized_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS recipe_ingredient (
    id BIGINT NOT NULL AUTO_INCREMENT,
    recipe_id BIGINT NOT NULL,
    ingredient_id BIGINT NULL,
    raw_name VARCHAR(255) NOT NULL,
    role VARCHAR(32) NOT NULL,
    is_optional BOOLEAN NOT NULL,
    raw_quantity VARCHAR(255) NULL,
    quantity_min DECIMAL(12,3) NULL,
    quantity_max DECIMAL(12,3) NULL,
    unit VARCHAR(32) NULL,
    quantity_parse_status VARCHAR(32) NOT NULL,
    source_section VARCHAR(32) NOT NULL,
    sort_order INT NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_recipe_ingredient_recipe FOREIGN KEY (recipe_id) REFERENCES recipe (id),
    CONSTRAINT fk_recipe_ingredient_ingredient FOREIGN KEY (ingredient_id) REFERENCES ingredient (id),
    INDEX idx_recipe_ingredient_recipe_order (recipe_id, sort_order),
    INDEX idx_recipe_ingredient_ingredient (ingredient_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS recipe_step (
    id BIGINT NOT NULL AUTO_INCREMENT,
    recipe_id BIGINT NOT NULL,
    step_no INT NOT NULL,
    content TEXT NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_recipe_step (recipe_id, step_no),
    CONSTRAINT fk_recipe_step_recipe FOREIGN KEY (recipe_id) REFERENCES recipe (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS recipe_media (
    id BIGINT NOT NULL AUTO_INCREMENT,
    recipe_id BIGINT NOT NULL,
    media_type VARCHAR(32) NOT NULL,
    source_path VARCHAR(512) NOT NULL,
    alt_text VARCHAR(255) NULL,
    sort_order INT NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_recipe_media_recipe FOREIGN KEY (recipe_id) REFERENCES recipe (id),
    INDEX idx_recipe_media_recipe_order (recipe_id, sort_order)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
