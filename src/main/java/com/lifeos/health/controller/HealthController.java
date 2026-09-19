package com.lifeos.health.controller;

import com.lifeos.common.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Health and diagnostics endpoint verifying application, database, and pgvector readiness.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/health")
@RequiredArgsConstructor
@Tag(name = "Health & Diagnostics", description = "System health, database connectivity, and vector capabilities")
public class HealthController {

    private final JdbcTemplate jdbcTemplate;

    @GetMapping("/ping")
    @Operation(summary = "System health and pgvector verification ping")
    public ResponseEntity<ApiResponse<Map<String, Object>>> ping() {
        Map<String, Object> statusMap = new HashMap<>();
        statusMap.put("status", "UP");
        statusMap.put("application", "LifeOS Backend");
        statusMap.put("version", "0.1.0-alpha");
        statusMap.put("timestamp", OffsetDateTime.now());

        // Check PostgreSQL connection
        try {
            String dbVersion = jdbcTemplate.queryForObject("SELECT version();", String.class);
            statusMap.put("database", "CONNECTED");
            statusMap.put("databaseVersion", dbVersion != null && dbVersion.length() > 30 ? dbVersion.substring(0, 30) + "..." : dbVersion);
        } catch (Exception e) {
            log.error("Database connectivity check failed: ", e);
            statusMap.put("database", "DISCONNECTED");
            statusMap.put("databaseError", e.getMessage());
        }

        // Check pgvector extension status
        try {
            Boolean vectorInstalled = jdbcTemplate.queryForObject(
                    "SELECT EXISTS (SELECT 1 FROM pg_extension WHERE extname = 'vector');", Boolean.class);
            statusMap.put("pgvectorExtension", Boolean.TRUE.equals(vectorInstalled) ? "AVAILABLE" : "MISSING");
        } catch (Exception e) {
            statusMap.put("pgvectorExtension", "NOT_CHECKED");
        }

        return ResponseEntity.ok(ApiResponse.success(statusMap, "LifeOS is healthy"));
    }
}
