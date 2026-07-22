package com.classification.domain_system.controller;

import com.classification.domain_system.integration.InboundIntegrationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/integration/inbound")
@RequiredArgsConstructor
public class InboundIntegrationController {

    private final InboundIntegrationService inboundService;

    @PostMapping("/{channelId}")
    public ResponseEntity<?> receiveInboundData(
            @PathVariable UUID channelId,
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestHeader(value = "X-API-KEY", required = false) String xApiKeyHeader,
            @RequestParam(value = "apiKey", required = false) String apiKeyParam,
            @RequestBody String rawPayload) {
        
        try {
            String resultPayload = inboundService.processInboundData(channelId, rawPayload, authHeader, xApiKeyHeader, apiKeyParam);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Inbound data processed successfully",
                    "resultPayload", resultPayload
            ));
        } catch (SecurityException se) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                    "success", false,
                    "message", se.getMessage()
            ));
        } catch (IllegalArgumentException iae) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", iae.getMessage()
            ));
        }
    }
}
