package com.classification.domain_system.config;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.context.annotation.Profile;
import org.springframework.transaction.annotation.Transactional;
import lombok.extern.slf4j.Slf4j;

@Component
@Profile("!test")
@Slf4j
public class DatabaseInitializer implements ApplicationRunner {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        try {
            // Create GIN Index for JSONB column in record table if not exists
            String createIndexSql = "CREATE INDEX IF NOT EXISTS idx_record_data_gin ON record USING GIN (CAST(data AS jsonb))";
            entityManager.createNativeQuery(createIndexSql).executeUpdate();
            log.info("Successfully ensured GIN index on record.data column for performance optimization.");
        } catch (Exception e) {
            log.warn("Failed to create GIN index on record.data. It may already exist or DB is not PostgreSQL: {}", e.getMessage());
        }
    }
}
