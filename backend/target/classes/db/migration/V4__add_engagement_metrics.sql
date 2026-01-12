-- Add engagement metrics to activity_records
ALTER TABLE activity_records 
ADD COLUMN active_seconds INTEGER DEFAULT 0,
ADD COLUMN scroll_depth INTEGER DEFAULT 0,
ADD COLUMN interaction_count INTEGER DEFAULT 0,
ADD COLUMN interest_score INTEGER DEFAULT 0;
