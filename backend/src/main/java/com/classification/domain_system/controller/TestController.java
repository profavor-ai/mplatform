package com.classification.domain_system.controller;

import com.classification.domain_system.entity.IntegrationChannel;
import com.classification.domain_system.repository.IntegrationChannelRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import lombok.RequiredArgsConstructor;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class TestController {

    private final IntegrationChannelRepository repository;

    @GetMapping("/api/test/channels")
    public List<IntegrationChannel> test() {
        return repository.findAll();
    }
}
