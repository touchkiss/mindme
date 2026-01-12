-- Enable vector extension
CREATE EXTENSION IF NOT EXISTS vector;

-- Table for Raw Activity Records (from Chrome Extension)
CREATE TABLE activity_records (
    id UUID PRIMARY KEY,
    url TEXT NOT NULL,
    title TEXT,
    visit_time TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    duration_seconds INTEGER,
    content_summary TEXT, -- AI summary from extension or backend
    page_content TEXT, -- Full content (optional/truncated)
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Table for Knowledge Nodes (Processed Knowledge)
CREATE TABLE knowledge_nodes (
    id UUID PRIMARY KEY,
    topic VARCHAR(255),
    content TEXT,
    source_url TEXT,
    embedding vector(1536), -- Vector embedding for search (OpenAI small model dim)
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Table for Reports
CREATE TABLE daily_reports (
    id UUID PRIMARY KEY,
    report_date DATE,
    summary_content TEXT,
    tags TEXT[],
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);
