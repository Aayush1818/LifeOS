package com.lifeos.system.controller;

import com.lifeos.common.dto.ApiResponse;
import com.lifeos.system.dto.SystemHealthResponse;
import com.lifeos.system.dto.SystemMetricsResponse;
import com.lifeos.system.service.SystemDiagnosticsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/system")
@RequiredArgsConstructor
@Tag(name = "System Diagnostics", description = "Probes and observability for database, pgvector, storage, and platform metrics")
public class SystemDiagnosticsController {

    private final SystemDiagnosticsService systemDiagnosticsService;

    @GetMapping("/health")
    @Operation(summary = "Get system health status", description = "Runs live health checks across PostgreSQL, pgvector extension, storage capacity, and Tika engine.")
    public ResponseEntity<ApiResponse<SystemHealthResponse>> getSystemHealth() {
        SystemHealthResponse health = systemDiagnosticsService.getSystemHealth();
        return ResponseEntity.ok(ApiResponse.success(health));
    }

    @GetMapping("/metrics")
    @Operation(summary = "Get platform metrics", description = "Provides aggregated entity counts, JVM memory usage, available processors, and disk capacity.")
    public ResponseEntity<ApiResponse<SystemMetricsResponse>> getSystemMetrics() {
        SystemMetricsResponse metrics = systemDiagnosticsService.getSystemMetrics();
        return ResponseEntity.ok(ApiResponse.success(metrics));
    }
}
