package com.touchkiss.mindme.domain;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.ZonedDateTime;
import java.util.UUID;

@Entity
@Table(name = "knowledge_edges")
@Data
@NoArgsConstructor
public class KnowledgeEdge {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_id", nullable = false)
    private KnowledgeNode source;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "target_id", nullable = false)
    private KnowledgeNode target;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private EdgeType type;

    private Double weight = 1.0;

    @Column(name = "created_at")
    private ZonedDateTime createdAt = ZonedDateTime.now();

    public enum EdgeType {
        RELATED, SUB_TOPIC, PREREQUISITE, MENTIONS
    }
}
