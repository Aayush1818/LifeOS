package com.lifeos.ai.agent.tool.domain;

import com.lifeos.ai.agent.tool.LifeOSTool;
import com.lifeos.ai.agent.tool.ToolExecutionResult;
import com.lifeos.ai.agent.tool.ToolParameterSchema;
import com.lifeos.loan.dto.LoanPortfolioSummaryResponse;
import com.lifeos.loan.service.LoanAnalyticsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

/**
 * Tool for deterministic aggregation of user's active loans and monthly EMI obligations.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LoanSummaryTool implements LifeOSTool {

    private final LoanAnalyticsService loanAnalyticsService;

    @Override
    public String getName() {
        return "get_loan_summary";
    }

    @Override
    public String getDescription() {
        return "Calculates total active loans count, total outstanding balance, and total monthly EMI commitment.";
    }

    @Override
    public ToolParameterSchema getParameterSchema() {
        return ToolParameterSchema.empty();
    }

    @Override
    public boolean requiresConfirmation() {
        return false;
    }

    @Override
    public ToolExecutionResult execute(UUID userId, Map<String, Object> parameters) {
        log.debug("Executing LoanSummaryTool for user [{}]", userId);
        try {
            LoanPortfolioSummaryResponse summary = loanAnalyticsService.getPortfolioSummary(userId);
            return ToolExecutionResult.success(summary);
        } catch (Exception e) {
            log.error("Failed to calculate loan summary for user [{}]: {}", userId, e.getMessage());
            return ToolExecutionResult.failure("Failed to calculate loan summary: " + e.getMessage());
        }
    }
}
