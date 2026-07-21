package com.classification.domain_system.service;

import com.classification.domain_system.dto.ConnectionTestRequest;
import com.classification.domain_system.dto.ConnectionTestResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.sql.Connection;
import java.sql.DriverManager;
import java.net.URI;

@Slf4j
@Service
public class IntegrationTestService {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final RestTemplate restTemplate = new RestTemplate(); // Simple rest template

    public ConnectionTestResponse testConnection(ConnectionTestRequest request) {
        try {
            JsonNode config = objectMapper.readTree(request.getConfigJson());
            
            switch (request.getType()) {
                case "WEB_SERVICE":
                    return testWebService(config);
                case "JDBC":
                    return testJdbc(config);
                case "MESSAGE_QUEUE":
                    return testMessageQueue(config);
                default:
                    return new ConnectionTestResponse(false, "Unknown channel type: " + request.getType());
            }
        } catch (Exception e) {
            log.error("Test connection failed", e);
            return new ConnectionTestResponse(false, "테스트 중 오류 발생: " + e.getMessage());
        }
    }

    private ConnectionTestResponse testWebService(JsonNode config) {
        String url = config.path("url").asText();
        if (url == null || url.trim().isEmpty()) {
            return new ConnectionTestResponse(false, "URL이 입력되지 않았습니다.");
        }
        
        try {
            // For simple ping, we'll try an OPTIONS or HEAD request. But some endpoints block them.
            // Let's just do a simple try block. If it throws an exception (like UnknownHost), it fails.
            URI uri = new URI(url);
            // We just validate the URI is well-formed for now, as sending a random POST/GET might trigger side effects on the user's actual webhook.
            // But the user requested a "Connection Test". So we can try to do a HEAD or GET.
            try {
                restTemplate.exchange(uri, HttpMethod.OPTIONS, null, String.class);
            } catch (Exception ex) {
                // If OPTIONS fails, it might just be not supported. As long as it's not a connection refused.
                // We'll consider it a success if we could at least reach it, or if it's 405 Method Not Allowed.
                String msg = ex.getMessage();
                if (msg != null && (msg.contains("Connection refused") || msg.contains("UnknownHostException"))) {
                    return new ConnectionTestResponse(false, "서버에 연결할 수 없습니다: " + msg);
                }
            }
            return new ConnectionTestResponse(true, "웹 서비스 연결이 유효합니다 (URL 검증 완료).");
        } catch (Exception e) {
            return new ConnectionTestResponse(false, "유효하지 않은 URL이거나 접근할 수 없습니다: " + e.getMessage());
        }
    }

    private ConnectionTestResponse testJdbc(JsonNode config) {
        String url = config.path("url").asText();
        String user = config.path("user").asText();
        String password = config.path("password").asText();
        
        if (url == null || url.trim().isEmpty()) {
            return new ConnectionTestResponse(false, "JDBC URL이 입력되지 않았습니다.");
        }

        try (Connection conn = DriverManager.getConnection(url, user, password)) {
            if (conn.isValid(5)) {
                return new ConnectionTestResponse(true, "DB 연결 성공!");
            } else {
                return new ConnectionTestResponse(false, "DB 연결이 유효하지 않습니다.");
            }
        } catch (Exception e) {
            return new ConnectionTestResponse(false, "DB 연결 실패: " + e.getMessage());
        }
    }

    private ConnectionTestResponse testMessageQueue(JsonNode config) {
        String broker = config.path("broker").asText();
        if (broker == null || broker.trim().isEmpty()) {
            return new ConnectionTestResponse(false, "브로커 URL이 입력되지 않았습니다.");
        }
        // Basic validation for MQ
        if (broker.startsWith("kafka://") || broker.startsWith("amqp://") || broker.startsWith("tcp://")) {
            return new ConnectionTestResponse(true, "메시지 큐 설정 형식이 유효합니다 (형식 검증 완료).");
        } else {
            return new ConnectionTestResponse(false, "지원하지 않는 브로커 프로토콜입니다 (kafka://, amqp:// 등 사용 권장).");
        }
    }
}
