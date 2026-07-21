package com.classification.domain_system.controller;

import com.classification.domain_system.entity.IntegrationChannel;
import com.classification.domain_system.repository.IntegrationChannelRepository;
import com.classification.domain_system.dto.ConnectionTestRequest;
import com.classification.domain_system.dto.ConnectionTestResponse;
import com.classification.domain_system.service.IntegrationTestService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/integration/channels")
@RequiredArgsConstructor
public class IntegrationChannelController {

    private final IntegrationChannelRepository repository;
    private final IntegrationTestService testService;

    @PostMapping("/test-connection")
    public ResponseEntity<ConnectionTestResponse> testConnection(@RequestBody ConnectionTestRequest request) {
        return ResponseEntity.ok(testService.testConnection(request));
    }

    @GetMapping
    public List<IntegrationChannel> getAll() {
        return repository.findAll();
    }

    @PostMapping
    public IntegrationChannel create(@RequestBody IntegrationChannel channel) {
        return repository.save(channel);
    }

    @PutMapping("/{id}")
    public ResponseEntity<IntegrationChannel> update(@PathVariable UUID id, @RequestBody IntegrationChannel updated) {
        return repository.findById(id).map(channel -> {
            channel.setName(updated.getName());
            channel.setType(updated.getType());
            channel.setNodeId(updated.getNodeId());
            channel.setConfigJson(updated.getConfigJson());
            channel.setMappingConfigJson(updated.getMappingConfigJson());
            channel.setActive(updated.isActive());
            return ResponseEntity.ok(repository.save(channel));
        }).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        if (repository.existsById(id)) {
            repository.deleteById(id);
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.notFound().build();
    }
}
