-- V3: Add user interests table for tracking interest profiles

CREATE TABLE user_interests (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    category VARCHAR(100) NOT NULL,
    weight DOUBLE PRECISION DEFAULT 1.0,
    last_updated TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(category)
);

CREATE TABLE user_tags (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tag VARCHAR(100) NOT NULL,
    frequency INTEGER DEFAULT 1,
    last_seen TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(tag)
);

CREATE INDEX idx_user_interests_weight ON user_interests(weight DESC);
CREATE INDEX idx_user_tags_frequency ON user_tags(frequency DESC);
