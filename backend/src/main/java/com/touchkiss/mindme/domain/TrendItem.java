package com.touchkiss.mindme.domain;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.ZonedDateTime;
import java.util.UUID;

@Data
@Entity
@NoArgsConstructor
@Table(name = "trend_items", indexes = {
        @Index(name = "idx_trend_item_url", columnList = "url", unique = true),
        @Index(name = "idx_trend_item_created_at", columnList = "created_at")
})
public class TrendItem {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String source;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String url;

    // Score from the source (e.g., upvotes, stars)
    private int score;

    // Calculated relevance score based on user interests
    @Column(name = "ai_score")
    private int aiScore;

    @Column(name = "created_at", nullable = false, updatable = false)
    private ZonedDateTime createdAt;

    public TrendItem(String source, String title, String url, int score) {
        this.source = source;
        this.title = title;
        this.url = url;
        this.score = score;
    }

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = ZonedDateTime.now();
        }
    }
}
