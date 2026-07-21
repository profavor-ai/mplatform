package com.classification.domain_system.dto;

import lombok.Data;

@Data
public class ConnectionTestRequest {
    private String type; // WEB_SERVICE, JDBC, MESSAGE_QUEUE
    private String configJson; // JSON string with configuration
}
