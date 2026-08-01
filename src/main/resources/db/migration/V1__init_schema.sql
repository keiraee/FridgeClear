-- FridgeClear initial schema.
-- Credentials and database creation are intentionally managed outside this file.

CREATE TABLE IF NOT EXISTS pantry_item (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    ingredient_id BIGINT NULL,
    raw_name VARCHAR(128) NOT NULL,
    quantity DECIMAL(12, 3) NULL,
    unit VARCHAR(32) NULL,
    purchase_date DATE NULL,
    expire_date DATE NULL,
    status VARCHAR(32) NOT NULL,
    note VARCHAR(255) NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    INDEX idx_pantry_item_user_status (user_id, status),
    INDEX idx_pantry_item_expire_date (expire_date),
    INDEX idx_pantry_item_ingredient (ingredient_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
