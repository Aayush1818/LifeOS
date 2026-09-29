package com.lifeos.insight.controller;

import com.lifeos.common.dto.ApiResponse;
import com.lifeos.common.security.SecurityUtils;
import com.lifeos.insight.dto.InsightResponse;
import com.lifeos.insight.dto.InsightSummaryResponse;
import com.lifeos.insight.entity.InsightSeverity;
import com.lifeos.insight.service.InsightService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/insights")
@RequiredArgsConstructor
@SecurityRequirement(name = "BearerAuth")
@Tag(name = "Proactive Life Insights", description = "Automated cross-domain anomaly detection, budget alerts, and financial optimizations")
public class InsightController {

    private final InsightService insightService;

    @GetMapping
    @Operation(summary = "Get active proactive insights", description = "Retrieves active non-dismissed anomalies and optimizations across all personal life domains.")
    public ResponseEntity<ApiResponse<InsightSummaryResponse>> getActiveInsights(
            @RequestParam(required = false) InsightSeverity minSeverity
    ) {
        UUID currentUserId = SecurityUtils.getCurrentUserId();
        InsightSummaryResponse response = insightService.getActiveInsights(currentUserId, minSeverity);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/generate")
    @Operation(summary = "Re-evaluate proactive analyzers", description = "Triggers on-demand re-evaluation of all cross-domain rule analyzers.")
    public ResponseEntity<ApiResponse<InsightSummaryResponse>> generateInsights() {
        UUID currentUserId = SecurityUtils.getCurrentUserId();
        InsightSummaryResponse response = insightService.generateInsights(currentUserId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/{id}/dismiss")
    @Operation(summary = "Dismiss an insight", description = "Marks a proactive insight as dismissed so it no longer appears in active views.")
    public ResponseEntity<ApiResponse<InsightResponse>> dismissInsight(@PathVariable UUID id) {
        UUID currentUserId = SecurityUtils.getCurrentUserId();
        InsightResponse response = insightService.dismissInsight(currentUserId, id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/{id}/action")
    @Operation(summary = "Action an insight", description = "Marks an insight as actioned following user remediation.")
    public ResponseEntity<ApiResponse<InsightResponse>> actionInsight(@PathVariable UUID id) {
        UUID currentUserId = SecurityUtils.getCurrentUserId();
        InsightResponse response = insightService.actionInsight(currentUserId, id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
