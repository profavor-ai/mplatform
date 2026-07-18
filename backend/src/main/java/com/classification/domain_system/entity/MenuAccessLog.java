package com.classification.domain_system.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;

@Entity
@Table(name = "menu_access_log")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class MenuAccessLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "menu_id")
    private Long menuId;

    @Column(name = "menu_path", length = 255)
    private String menuPath;

    @Column(name = "user_id", length = 50)
    private String userId;

    @Column(name = "user_agent", length = 512)
    private String userAgent;

    @Column(name = "client_ip", length = 45)
    private String clientIp;

    @CreationTimestamp
    @Column(name = "accessed_at", updatable = false)
    private LocalDateTime accessedAt;
}
