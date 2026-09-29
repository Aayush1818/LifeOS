package com.lifeos.system.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SystemHealthResponse {
    private String status; // "UP", "DEGRADED", "DOWN"
    private long uptimeSeconds;
    @Builder.Default
    private Instant timestamp = Instant.now();
    @Builder.Default
    private Map<String, ComponentHealth> components = new HashMap<>();
}
