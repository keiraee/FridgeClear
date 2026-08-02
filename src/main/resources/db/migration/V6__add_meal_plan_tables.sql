CREATE TABLE IF NOT EXISTS ai_plan_run (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    model_name VARCHAR(128) NOT NULL,
    prompt_version VARCHAR(32) NOT NULL,
    request_json LONGTEXT NOT NULL,
    response_json LONGTEXT NULL,
    status VARCHAR(32) NOT NULL,
    error_message TEXT NULL,
    started_at DATETIME(6) NOT NULL,
    finished_at DATETIME(6) NULL,
    PRIMARY KEY (id),
    INDEX idx_ai_plan_run_user_started (user_id, started_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS meal_plan (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    ai_plan_run_id BIGINT NULL,
    title VARCHAR(128) NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    status VARCHAR(32) NOT NULL,
    constraints_json LONGTEXT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_meal_plan_ai_run FOREIGN KEY (ai_plan_run_id) REFERENCES ai_plan_run (id),
    INDEX idx_meal_plan_user_date (user_id, start_date, end_date),
    INDEX idx_meal_plan_user_status (user_id, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS meal_plan_item (
    id BIGINT NOT NULL AUTO_INCREMENT,
    meal_plan_id BIGINT NOT NULL,
    plan_date DATE NOT NULL,
    meal_type VARCHAR(32) NOT NULL,
    recipe_id BIGINT NOT NULL,
    servings DECIMAL(8,2) NULL,
    used_ingredients_json LONGTEXT NULL,
    missing_ingredients_json LONGTEXT NULL,
    reason TEXT NULL,
    status VARCHAR(32) NOT NULL,
    sort_order INT NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_meal_plan_item_plan FOREIGN KEY (meal_plan_id) REFERENCES meal_plan (id),
    CONSTRAINT fk_meal_plan_item_recipe FOREIGN KEY (recipe_id) REFERENCES recipe (id),
    INDEX idx_meal_plan_item_plan_order (meal_plan_id, plan_date, sort_order)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS shopping_list_item (
    id BIGINT NOT NULL AUTO_INCREMENT,
    meal_plan_id BIGINT NOT NULL,
    ingredient_id BIGINT NULL,
    name VARCHAR(128) NOT NULL,
    quantity DECIMAL(12,3) NULL,
    unit VARCHAR(32) NULL,
    reason VARCHAR(255) NULL,
    status VARCHAR(32) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_shopping_list_plan FOREIGN KEY (meal_plan_id) REFERENCES meal_plan (id),
    CONSTRAINT fk_shopping_list_ingredient FOREIGN KEY (ingredient_id) REFERENCES ingredient (id),
    INDEX idx_shopping_list_plan_status (meal_plan_id, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
