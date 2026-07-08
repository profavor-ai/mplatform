package com.classification.domain_system.dto;

import lombok.Data;
import java.util.Map;
import java.util.UUID;

@Data
public class FieldGroupRequest {
    private UUID sectorId;
    private Map<String, String> name;
    private Integer sortOrder;
    private Boolean isDefaultOpen;
}
