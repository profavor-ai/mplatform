package com.classification.domain_system.dto;

import lombok.Data;
import java.util.Map;

@Data
public class SectorRequest {
    private Map<String, String> name;
    private Integer sortOrder;
}
