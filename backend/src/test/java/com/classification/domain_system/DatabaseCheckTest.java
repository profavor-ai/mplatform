package com.classification.domain_system;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.Map;

import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
public class DatabaseCheckTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    public void printDatabaseData() {
        System.out.println("====================================================");
        System.out.println("[DB CHECK] Domain Table Info:");
        List<Map<String, Object>> domains = jdbcTemplate.queryForList("SELECT id, name, identifier_field_id, display_name_field_id, numbering_pattern, current_sequence FROM \"domain\"");
        for (Map<String, Object> d : domains) {
            System.out.println("Domain -> ID: " + d.get("id") + ", Name: " + d.get("name") + ", IdentifierFieldId: " + d.get("identifier_field_id") + ", Pattern: " + d.get("numbering_pattern"));
        }

        System.out.println("[DB CHECK] Field Definition Table Info:");
        List<Map<String, Object>> fields = jdbcTemplate.queryForList("SELECT id, domain_id, field_key, name, required, type FROM field_definition");
        for (Map<String, Object> f : fields) {
            System.out.println("Field -> ID: " + f.get("id") + ", DomainID: " + f.get("domain_id") + ", Key: " + f.get("field_key") + ", Name: " + f.get("name") + ", Required: " + f.get("required") + ", Type: " + f.get("type"));
        }
        System.out.println("====================================================");
    }
}
