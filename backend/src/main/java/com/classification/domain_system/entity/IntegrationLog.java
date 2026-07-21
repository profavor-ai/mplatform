package com.classification.domain_system.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "integration_logs")
@Getter
@Setter
public class IntegrationLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID channelId;

    @Column(nullable = false)
    private UUID recordId;

    @Column(nullable = false, length = 50)
    private String eventType; // CREATE, UPDATE, DELETE

    @Column(columnDefinition = "TEXT")
    private String originalPayload;

    @Column(columnDefinition = "TEXT")
    private String mappedPayload;

    @Column(nullable = false, length = 20)
    private String status; // SUCCESS, FAIL

    @Column(columnDefinition = "TEXT")
    private String errorMessage;

    @Column(columnDefinition = "TEXT")
    private String stackTrace;

    @Column(nullable = false)
    private int retryCount = 0;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
