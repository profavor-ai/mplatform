package com.classification.domain_system.controller;

import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;
import com.classification.domain_system.repository.RecordRepository;
import com.classification.domain_system.entity.Record;
import java.util.*;

@RestController
@RequestMapping("/api/dev")
public class TestController {

    @Autowired
    private RecordRepository recordRepository;

    @GetMapping("/test-dup")
    public Object testDup(@RequestParam UUID nodeId, @RequestParam String key, @RequestParam String val) {
        Map<String, String> searchParams = new HashMap<>();
        searchParams.put(key, val);
        searchParams.put("op_" + key, "EQ");
        
        List<Record> duplicates = recordRepository.findDynamicRecords(List.of(nodeId), null, searchParams);
        
        Map<String, Object> result = new HashMap<>();
        result.put("found_count", duplicates.size());
        return result;
    }

    @GetMapping("/dump")
    public Object dump() {
        return recordRepository.findAll();
    }
}
