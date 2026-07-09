package com.classification.domain_system.controller;

import org.springframework.web.bind.annotation.*;
import org.springframework.jdbc.core.JdbcTemplate;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class DbFixController {
    
    private final JdbcTemplate jdbcTemplate;
    
    @GetMapping("/api/dev/fix-db")
    public String fixDb() {
        try {
            jdbcTemplate.execute("ALTER TABLE record_history ADD COLUMN version INTEGER DEFAULT 1");
            jdbcTemplate.execute("UPDATE record_history SET version = 1 WHERE version IS NULL");
            jdbcTemplate.execute("ALTER TABLE record_history ALTER COLUMN version SET NOT NULL");
            return "Fixed DB!";
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }
}
