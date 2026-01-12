package com.touchkiss.mindme.domain;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Entity
@Table(name = "user_configs")
public class UserConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(nullable = false, unique = true)
    private String configKey;

    @Column(columnDefinition = "TEXT")
    private String configValue;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
