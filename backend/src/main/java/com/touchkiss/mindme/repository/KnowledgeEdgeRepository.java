package com.touchkiss.mindme.repository;

import com.touchkiss.mindme.domain.KnowledgeEdge;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface KnowledgeEdgeRepository extends JpaRepository<KnowledgeEdge, UUID> {
    List<KnowledgeEdge> findBySourceId(UUID sourceId);

    List<KnowledgeEdge> findByTargetId(UUID targetId);

    java.util.Optional<KnowledgeEdge> findBySourceAndTarget(com.touchkiss.mindme.domain.KnowledgeNode source,
            com.touchkiss.mindme.domain.KnowledgeNode target);
}
