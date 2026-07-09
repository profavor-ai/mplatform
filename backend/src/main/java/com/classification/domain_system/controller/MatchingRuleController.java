package com.classification.domain_system.controller;

import com.classification.domain_system.entity.MatchingRule;
import com.classification.domain_system.repository.MatchingRuleRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/domains/{domainId}/matching-rules")
@RequiredArgsConstructor
public class MatchingRuleController {

    private final MatchingRuleRepository matchingRuleRepository;

    @GetMapping
    public ResponseEntity<List<MatchingRule>> getRules(@PathVariable UUID domainId) {
        return ResponseEntity.ok(matchingRuleRepository.findByDomainId(domainId));
    }

    @PostMapping
    public ResponseEntity<MatchingRule> createRule(@PathVariable UUID domainId, @RequestBody MatchingRule rule) {
        rule.setDomainId(domainId);
        return ResponseEntity.ok(matchingRuleRepository.save(rule));
    }

    @PutMapping("/{ruleId}")
    public ResponseEntity<MatchingRule> updateRule(@PathVariable UUID domainId, @PathVariable UUID ruleId, @RequestBody MatchingRule rule) {
        MatchingRule existing = matchingRuleRepository.findById(ruleId).orElseThrow(() -> new RuntimeException("Rule not found"));
        existing.setRuleName(rule.getRuleName());
        existing.setTargetFieldKeys(rule.getTargetFieldKeys());
        existing.setMatchType(rule.getMatchType());
        existing.setActive(rule.isActive());
        return ResponseEntity.ok(matchingRuleRepository.save(existing));
    }

    @DeleteMapping("/{ruleId}")
    public ResponseEntity<Void> deleteRule(@PathVariable UUID domainId, @PathVariable UUID ruleId) {
        matchingRuleRepository.deleteById(ruleId);
        return ResponseEntity.ok().build();
    }
}
