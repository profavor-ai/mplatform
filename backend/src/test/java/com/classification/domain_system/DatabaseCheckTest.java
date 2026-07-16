package com.classification.domain_system;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.Map;

@SpringBootTest
public class DatabaseCheckTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    public void printDatabaseData() {
        System.out.println("====================================================");
        System.out.println("[DB CHECK] Users Table Info:");
        List<Map<String, Object>> users = jdbcTemplate.queryForList("SELECT id, username, role FROM users");
        for (Map<String, Object> u : users) {
            System.out.println("User -> ID: " + u.get("id") + ", Username: " + u.get("username") + ", Role: " + u.get("role"));
        }

        System.out.println("[DB CHECK] Recent Approval Requests:");
        List<Map<String, Object>> requests = jdbcTemplate.queryForList(
                "SELECT id, status, requester_id, target_type FROM approval_request ORDER BY created_at DESC LIMIT 5");
        for (Map<String, Object> req : requests) {
            System.out.println("Request -> ID: " + req.get("id") + ", Status: " + req.get("status") 
                    + ", RequesterID: " + req.get("requester_id") + ", Type: " + req.get("target_type"));
            
            List<Map<String, Object>> steps = jdbcTemplate.queryForList(
                    "SELECT id, step_order, step_type, assignee_id, status FROM approval_step WHERE request_id = ? ORDER BY step_order",
                    req.get("id"));
            for (Map<String, Object> step : steps) {
                System.out.println("  └ Step -> Order: " + step.get("step_order") + ", Type: " + step.get("step_type")
                        + ", AssigneeID: " + step.get("assignee_id") + ", Status: " + step.get("status"));
            }
        }
        System.out.println("====================================================");
    }
}
