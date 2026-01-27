package com.touchkiss.mindme.domain;

import jakarta.persistence.*;
import lombok.Data;
import java.time.ZonedDateTime;
import java.util.UUID;

@Data
@Entity
@Table(name = "watched_pages")
public class WatchedPage {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String url;

    private String title;

    @Column(name = "last_content_hash")
    private String lastContentHash;

    @Column(name = "last_content_summary", columnDefinition = "TEXT")
    private String lastContentSummary;

    @Column(name = "last_checked")
    private ZonedDateTime lastChecked;

    @Column(name = "check_interval_hours")
    private Integer checkIntervalHours = 24; // Default: check daily

    @Column(name = "has_updates")
    private Boolean hasUpdates = false;

    @Column(name = "update_count")
    private Integer updateCount = 0;

    @Column(name = "is_active")
    private Boolean isActive = true;

    @Column(name = "created_at", updatable = false)
    private ZonedDateTime createdAt;

    @Column(name = "updated_at")
    private ZonedDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = ZonedDateTime.now();
        }
        updatedAt = ZonedDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = ZonedDateTime.now();
    }
}
