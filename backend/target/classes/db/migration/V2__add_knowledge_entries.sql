-- V2__add_knowledge_entries.sql

-- Add analyzed flag to activity_records
ALTER TABLE activity_records ADD COLUMN IF NOT EXISTS analyzed BOOLEAN DEFAULT FALSE;

-- Create knowledge_entries table
CREATE TABLE IF NOT EXISTS knowledge_entries (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    title VARCHAR(500) NOT NULL,
    content TEXT,
    category VARCHAR(100),
    tags TEXT[],
    source_record_id UUID REFERENCES activity_records(id) ON DELETE SET NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Create index for faster lookups
CREATE INDEX IF NOT EXISTS idx_activity_records_analyzed ON activity_records(analyzed);
CREATE INDEX IF NOT EXISTS idx_knowledge_entries_category ON knowledge_entries(category);
CREATE INDEX IF NOT EXISTS idx_knowledge_entries_source ON knowledge_entries(source_record_id);
