-- Knowledge Graph Nodes
DROP TABLE IF EXISTS knowledge_edges;
DROP TABLE IF EXISTS knowledge_nodes;

CREATE TABLE knowledge_nodes (
    id UUID PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    type VARCHAR(50) NOT NULL, -- e.g., 'CONCEPT', 'PAGE', 'PERSON'
    description TEXT,
    weight INTEGER DEFAULT 0, -- Importance or frequency
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_knowledge_nodes_name ON knowledge_nodes(name);
CREATE INDEX idx_knowledge_nodes_type ON knowledge_nodes(type);

-- Knowledge Graph Edges
CREATE TABLE knowledge_edges (
    id UUID PRIMARY KEY,
    source_id UUID NOT NULL REFERENCES knowledge_nodes(id) ON DELETE CASCADE,
    target_id UUID NOT NULL REFERENCES knowledge_nodes(id) ON DELETE CASCADE,
    type VARCHAR(50) NOT NULL, -- e.g., 'RELATED', 'SUB_TOPIC', 'PREREQUISITE'
    weight FLOAT DEFAULT 1.0, -- Strength of relationship
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT uq_knowledge_edges_source_target UNIQUE(source_id, target_id, type)
);

CREATE INDEX idx_knowledge_edges_source ON knowledge_edges(source_id);
CREATE INDEX idx_knowledge_edges_target ON knowledge_edges(target_id);
