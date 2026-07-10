package com.profavor.domain.dataquality.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "data_quality_rules")
@Getter
@Setter
@EntityListeners(AuditingEntityListener.class)
public class DataQualityRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false)
    private String tenantId;

    @Column(name = "domain_type", nullable = false)
    private String domainType; // e.g. "CUSTOMER", "PRODUCT"

    @Column(name = "field_name", nullable = false)
    private String fieldName;

    @Column(name = "rule_type", nullable = false)
    private String ruleType; // e.g. "REGEX", "NOT_NULL", "CUSTOM"

    @Column(name = "rule_value")
    private String ruleValue;

    @Column(name = "is_active")
    private boolean isActive = true;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
