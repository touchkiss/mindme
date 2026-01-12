package com.touchkiss.mindme.repository;

import com.touchkiss.mindme.domain.KnowledgeNode;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface KnowledgeNodeRepository extends JpaRepository<KnowledgeNode, UUID> {
    Optional<KnowledgeNode> findByNameAndType(String name, KnowledgeNode.NodeType type);
}
