package com.lifeos.ai.agent.tool.domain;

import com.lifeos.ai.agent.tool.LifeOSTool;
import com.lifeos.ai.agent.tool.ToolExecutionResult;
import com.lifeos.ai.agent.tool.ToolParameter;
import com.lifeos.ai.agent.tool.ToolParameterSchema;
import com.lifeos.insight.dto.InsightSummaryResponse;
import com.lifeos.insight.entity.InsightSeverity;
import com.lifeos.insight.service.InsightService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

/**
 * Deterministic AI agent tool for retrieving cross-domain anomalies and optimizations.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ProactiveInsightsTool implements LifeOSTool {

    private final InsightService insightService;

    @Override
    public String getName() {
        return "get_proactive_insights";
    }

    @Override
    public String getDescription() {
        return "Analyzes cross-domain data to detect proactive financial anomalies, spending surges, budget depletion risks, high-interest debt optimizations, and coverage gaps.";
    }

    @Override
    public ToolParameterSchema getParameterSchema() {
        return ToolParameterSchema.of(
                ToolParameter.builder()
                        .name("minSeverity")
                        .type("string")
                        .description("Minimum severity filter: INFO, WARNING, or CRITICAL. Defaults to INFO.")
                        .required(false)
                        .build()
        );
    }

    @Override
    public boolean requiresConfirmation() {
        return false;
    }

    @Override
    public ToolExecutionResult execute(UUID userId, Map<String, Object> parameters) {
        log.debug("Executing ProactiveInsightsTool for user [{}]", userId);
        try {
            InsightSeverity minSeverity = null;
            if (parameters != null && parameters.get("minSeverity") != null) {
                try {
                    minSeverity = InsightSeverity.valueOf(parameters.get("minSeverity").toString().trim().toUpperCase());
                } catch (IllegalArgumentException ignored) {}
            }

            InsightSummaryResponse summary = insightService.getActiveInsights(userId, minSeverity);
            return ToolExecutionResult.success(summary);
        } catch (Exception e) {
            log.error("Failed to execute ProactiveInsightsTool for user [{}]: {}", userId, e.getMessage());
            return ToolExecutionResult.failure("Failed to retrieve proactive insights: " + e.getMessage());
        }
    }
}
