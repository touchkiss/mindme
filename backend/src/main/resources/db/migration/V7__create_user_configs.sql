CREATE TABLE IF NOT EXISTS user_configs (
    id UUID PRIMARY KEY,
    config_key VARCHAR(255) NOT NULL UNIQUE,
    config_value TEXT,
    updated_at TIMESTAMP
);
