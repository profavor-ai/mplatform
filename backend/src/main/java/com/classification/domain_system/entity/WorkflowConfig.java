package com.classification.domain_system.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import java.util.UUID;
import java.time.LocalDateTime;

@Entity
@Table(name = "workflow_config", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"domain_id", "node_id", "action_type"})
})
@Getter
@Setter
@NoArgsConstructor
public class WorkflowConfig {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    // A workflow can be bound to either a Domain or a specific ClassificationNode.
    @Column(name = "domain_id")
    private UUID domainId;

    @Column(name = "node_id")
    private UUID nodeId;

    @Column(name = "action_type", nullable = false, length = 20)
    private String actionType; // CREATE, UPDATE, DELETE

    @Column(name = "steps_config", columnDefinition = "text")
    private String stepsConfig;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = this.createdAt;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
