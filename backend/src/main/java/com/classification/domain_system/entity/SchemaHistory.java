package com.classification.domain_system.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "schema_history")
@Getter
@Setter
@NoArgsConstructor
public class SchemaHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "domain_id")
    private UUID domainId;

    @Column(name = "target_type", nullable = false, length = 30)
    private String targetType; // DOMAIN, NODE, FIELD, GROUP

    @Column(name = "target_id", nullable = false)
    private UUID targetId;

    @Column(name = "action", nullable = false, length = 20)
    private String action; // CREATE, UPDATE, DELETE

    @Column(name = "before_data", columnDefinition = "TEXT")
    private String beforeData;

    @Column(name = "after_data", columnDefinition = "TEXT")
    private String afterData;

    @Column(name = "changed_by", length = 100)
    private String changedBy;

    @Column(name = "changed_at", updatable = false)
    private LocalDateTime changedAt;

    @PrePersist
    protected void onCreate() {
        this.changedAt = LocalDateTime.now();
    }
}
