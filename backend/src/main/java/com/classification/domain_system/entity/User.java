package com.classification.domain_system.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.util.UUID;
import org.hibernate.annotations.UuidGenerator;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class User {
    
    @Id
    @UuidGenerator
    private String id;
    
    private String username;
    private String password;
    private String role;
    private String timezone;

    private UUID organizationId;
    private UUID departmentId;
    private UUID teamId;
    private Boolean isActive = true;
}
