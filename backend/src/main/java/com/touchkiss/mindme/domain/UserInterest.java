package com.touchkiss.mindme.domain;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Entity
@Table(name = "user_interests")
public class UserInterest {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(nullable = false, unique = true)
    private String category;

    @Column(nullable = false)
    private Double weight = 1.0;

    @UpdateTimestamp
    private LocalDateTime lastUpdated;

    @CreationTimestamp
    private LocalDateTime createdAt;
}
