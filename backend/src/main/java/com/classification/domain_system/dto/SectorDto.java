package com.classification.domain_system.dto;

import lombok.Data;
import java.util.Map;
import java.util.UUID;

@Data
public class SectorDto {
    private UUID id;
    private UUID domainId;
    private Map<String, String> name;
    private Integer sortOrder;
}
