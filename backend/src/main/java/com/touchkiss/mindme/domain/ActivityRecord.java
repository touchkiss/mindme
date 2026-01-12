package com.touchkiss.mindme.domain;

import jakarta.persistence.*;
import lombok.Data;
import java.time.ZonedDateTime;
import java.util.UUID;

@Data
@Entity
@Table(name = "activity_records")
public class ActivityRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String url;

    private String title;

    @Column(name = "visit_time")
    private ZonedDateTime visitTime;

    @Column(name = "duration_seconds")
    private Integer durationSeconds;

    @Column(name = "content_summary", columnDefinition = "TEXT")
    private String contentSummary;

    @Column(name = "page_content", columnDefinition = "TEXT")
    private String pageContent;

    @Column(name = "analyzed")
    private Boolean analyzed = false;

    @Column(name = "active_seconds")
    private Integer activeSeconds = 0;

    @Column(name = "scroll_depth")
    private Integer scrollDepth = 0;

    @Column(name = "interaction_count")
    private Integer interactionCount = 0;

    @Column(name = "interest_score")
    private Integer interestScore = 0;

    @Column(name = "referrer")
    private String referrer;

    @Column(name = "related_record_url")
    private String relatedRecordUrl;

    @Column(name = "relationship_type")
    private String relationshipType;

    @Column(name = "transition_type")
    private String transitionType;

    @Column(name = "search_query")
    private String searchQuery;

    @Column(name = "created_at", updatable = false)
    private ZonedDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = ZonedDateTime.now();
        }
    }
}
