package com.touchkiss.mindme.domain;

import jakarta.persistence.*;
import lombok.Data;
import java.time.ZonedDateTime;
import java.util.UUID;

@Data
@Entity
@Table(name = "reading_queue")
public class ReadingQueue {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 2048)
    private String url;

    @Column(length = 1024)
    private String title;

    @Column(name = "added_at")
    private ZonedDateTime addedAt;

    @Column(name = "read_at")
    private ZonedDateTime readAt;

    private Integer priority = 0;

    @Column(name = "is_read")
    private Boolean isRead = false;

    @PrePersist
    protected void onCreate() {
        if (addedAt == null) {
            addedAt = ZonedDateTime.now();
        }
    }
}
