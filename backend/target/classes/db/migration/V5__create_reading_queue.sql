-- Reading Queue Table
CREATE TABLE reading_queue (
    id UUID PRIMARY KEY,
    url VARCHAR(2048) NOT NULL,
    title VARCHAR(1024),
    added_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    read_at TIMESTAMP WITH TIME ZONE,
    priority INTEGER DEFAULT 0,
    is_read BOOLEAN DEFAULT FALSE
);

CREATE INDEX idx_reading_queue_is_read ON reading_queue(is_read);
