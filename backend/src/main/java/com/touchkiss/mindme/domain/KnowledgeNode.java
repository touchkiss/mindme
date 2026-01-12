package com.touchkiss.mindme.domain;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.ZonedDateTime;
import java.util.UUID;

@Entity
@Table(name = "knowledge_nodes")
@Data
@NoArgsConstructor
public class KnowledgeNode {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private NodeType type;

    @Column(columnDefinition = "TEXT")
    private String description;

    private Integer weight = 0;

    @Column(name = "created_at")
    private ZonedDateTime createdAt = ZonedDateTime.now();

    @Column(name = "updated_at")
    private ZonedDateTime updatedAt = ZonedDateTime.now();

    public enum NodeType {
        CONCEPT, PAGE, PERSON, TAG, PROJECT, TOPIC
    }
}
