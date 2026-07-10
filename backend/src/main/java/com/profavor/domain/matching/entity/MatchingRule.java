package com.profavor.domain.matching.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "matching_rules")
@Getter
@Setter
@EntityListeners(AuditingEntityListener.class)
public class MatchingRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false)
    private String tenantId;

    @Column(name = "domain_type", nullable = false)
    private String domainType;

    @Column(name = "match_fields")
    private String matchFields; // JSON array or comma separated fields

    @Column(name = "similarity_threshold")
    private Double similarityThreshold;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
