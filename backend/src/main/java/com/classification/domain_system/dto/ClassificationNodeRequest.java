package com.classification.domain_system.dto;

import lombok.Data;
import java.util.UUID;

import java.util.Map;

@Data
public class ClassificationNodeRequest {
    private UUID parentId;
    private Map<String, String> name;
    private Integer order;
    private String icon;
}
