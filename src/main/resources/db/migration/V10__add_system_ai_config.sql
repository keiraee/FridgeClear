CREATE TABLE IF NOT EXISTS system_ai_config (
    id BIGINT NOT NULL AUTO_INCREMENT,
    provider_name VARCHAR(128) NOT NULL,
    protocol VARCHAR(32) NOT NULL,
    base_url VARCHAR(512) NOT NULL,
    api_key_ciphertext TEXT NOT NULL,
    model_name VARCHAR(128) NOT NULL,
    enabled BOOLEAN NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
