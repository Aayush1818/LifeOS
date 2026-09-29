package com.lifeos.system.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ComponentHealth {
    private String status; // "UP", "DEGRADED", "DOWN"
    private Long latencyMs;
    private String details;
}
