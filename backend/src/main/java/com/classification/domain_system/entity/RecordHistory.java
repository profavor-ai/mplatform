package com.classification.domain_system.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "record_history")
@Getter
@Setter
@NoArgsConstructor
public class RecordHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "record_id", nullable = false)
    private UUID recordId;

    @Column(name = "change_type", nullable = false, length = 20)
    private String changeType; // CREATE, UPDATE, DELETE

    @Column(name = "changed_by", nullable = false)
    private UUID changedBy;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "previous_data")
    private String previousData;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "new_data")
    private String newData;

    @Column(name = "approval_request_id")
    private UUID approvalRequestId;

    @Column(name = "changed_at", nullable = false, updatable = false)
    private LocalDateTime changedAt;

    @PrePersist
    protected void onCreate() {
        this.changedAt = LocalDateTime.now();
    }
}
