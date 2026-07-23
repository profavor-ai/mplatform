package com.classification.domain_system.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "permission_item")
@Getter
@Setter
@NoArgsConstructor
public class PermissionItem {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id")
    @JsonIgnore
    private PermissionGroup group;

    @Column(name = "label_ko", nullable = false)
    private String labelKo;

    @Column(name = "label_en")
    private String labelEn;

    @Column(name = "perm_value", nullable = false)
    private String permValue;

    @Column(name = "sort_order")
    private Integer sortOrder = 0;
}
