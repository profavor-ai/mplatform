package com.classification.domain_system.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;

@Entity
@Table(name = "error_log")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ErrorLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", length = 50)
    private String userId;

    @Column(name = "request_uri", length = 255)
    private String requestUri;

    @Column(name = "error_message", length = 1000)
    private String errorMessage;

    @Column(name = "stack_trace", columnDefinition = "TEXT")
    private String stackTrace;

    @CreationTimestamp
    @Column(name = "logged_at", updatable = false)
    private LocalDateTime loggedAt;
}
