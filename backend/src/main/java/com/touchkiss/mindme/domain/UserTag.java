package com.touchkiss.mindme.domain;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Entity
@Table(name = "user_tags")
public class UserTag {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(nullable = false, unique = true)
    private String tag;

    @Column(nullable = false)
    private Integer frequency = 1;

    @UpdateTimestamp
    private LocalDateTime lastSeen;

    @CreationTimestamp
    private LocalDateTime createdAt;
}
